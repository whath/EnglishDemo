package com.englishcoach60.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.sqrt

sealed interface SpeechState {
    data object Idle : SpeechState
    data class Preparing(val progressPercent: Int) : SpeechState
    data object Listening : SpeechState
    data object Processing : SpeechState
    data class Result(val text: String, val durationMs: Long) : SpeechState
    data class Error(val message: String) : SpeechState
}

interface SpeechController {
    val state: StateFlow<SpeechState>

    fun setLocale(tag: String)
    fun start()
    fun stop()
    fun cancel()
    fun consumeResult()
    fun destroy()
}

class SherpaSpeechController(context: Context) : SpeechController {
    private val appContext = context.applicationContext
    private val modelStore = SherpaModelStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val recognizerMutex = Mutex()
    private val lock = Any()
    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val state: StateFlow<SpeechState> = mutableState.asStateFlow()

    private var recognizer: OfflineRecognizer? = null
    private var sessionId = 0L
    private var preparingJob: Job? = null
    private var recordingJob: Job? = null
    private var processingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var activeEngine: OfflineRecognizer? = null
    private var activeCollector: FloatChunkCollector? = null
    @Volatile private var recording = false

    override fun setLocale(tag: String) {
        // Parakeet-TDT is an English model. Accent selection remains relevant to TTS only.
    }

    override fun start() {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mutableState.value = SpeechState.Error("Microphone permission is required for speech recognition.")
            return
        }

        val token = synchronized(lock) {
            if (mutableState.value is SpeechState.Preparing ||
                mutableState.value == SpeechState.Listening ||
                mutableState.value == SpeechState.Processing
            ) {
                return
            }
            sessionId += 1
            sessionId
        }

        preparingJob = scope.launch {
            try {
                publish(token, SpeechState.Preparing(0))
                val paths = modelStore.ensureReady { progress ->
                    publish(token, SpeechState.Preparing(progress))
                }
                val engine = recognizerMutex.withLock {
                    recognizer ?: createRecognizer(paths).also { recognizer = it }
                }
                currentCoroutineContext().ensureActive()
                if (isActive(token)) beginRecording(token, engine)
            } catch (_: CancellationException) {
                // Cancellation is expected when leaving a step or changing difficulty.
            } catch (error: Throwable) {
                fail(token, preparationErrorMessage(error))
            }
        }
    }

    override fun stop() {
        val token = synchronized(lock) { sessionId }
        requestStop(token)
    }

    override fun cancel() {
        val record: AudioRecord?
        synchronized(lock) {
            sessionId += 1
            recording = false
            preparingJob?.cancel()
            recordingJob?.cancel()
            processingJob?.cancel()
            preparingJob = null
            recordingJob = null
            processingJob = null
            activeEngine = null
            activeCollector = null
            record = audioRecord
            audioRecord = null
        }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        mutableState.value = SpeechState.Idle
    }

    override fun consumeResult() {
        if (mutableState.value !is SpeechState.Preparing &&
            mutableState.value != SpeechState.Listening &&
            mutableState.value != SpeechState.Processing
        ) {
            mutableState.value = SpeechState.Idle
        }
    }

    override fun destroy() {
        cancel()
        scope.coroutineContext[Job]?.cancel()
        synchronized(lock) {
            recognizer?.release()
            recognizer = null
        }
    }

    private fun beginRecording(token: Long, engine: OfflineRecognizer) {
        val minimumBuffer = AudioRecord.getMinBufferSize(
            ParakeetEnglishModel.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimumBuffer > 0) { "The microphone does not support 16 kHz mono recording." }
        val bufferBytes = max(minimumBuffer, RECORDING_BUFFER_BYTES)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            ParakeetEnglishModel.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "The microphone could not be initialized."
        }

        try {
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "The microphone could not start recording."
            }
        } catch (error: Throwable) {
            record.release()
            throw error
        }

        val collector = FloatChunkCollector()
        synchronized(lock) {
            if (!isActive(token)) {
                record.stop()
                record.release()
                return
            }
            audioRecord = record
            activeEngine = engine
            activeCollector = collector
            recording = true
            processingJob = null
        }
        val captureJob = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            val pcm = ShortArray(bufferBytes / Short.SIZE_BYTES)
            var captureFailed = false
            try {
                while (recording && isActive(token)) {
                    val count = record.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) {
                        captureFailed = recording
                        break
                    }
                    collector.add(pcm, count)
                    if (collector.sampleCount >= MAX_RECORDING_SAMPLES) {
                        scope.launch { requestStop(token, engine, collector) }
                        break
                    }
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Throwable) {
                captureFailed = recording
            } finally {
                if (captureFailed) {
                    scope.launch { failCapture(token) }
                }
            }
        }
        synchronized(lock) {
            if (!isActive(token)) {
                captureJob.cancel()
                return
            }
            recordingJob = captureJob
        }
        publish(token, SpeechState.Listening)
        captureJob.start()
    }

    private fun requestStop(
        token: Long,
        engineOverride: OfflineRecognizer? = null,
        collectorOverride: FloatChunkCollector? = null,
    ) {
        val job = synchronized(lock) {
            if (!isActive(token) || mutableState.value != SpeechState.Listening || processingJob != null) return
            val record = audioRecord ?: return
            val engine = engineOverride ?: activeEngine ?: return
            val collector = collectorOverride ?: activeCollector ?: return
            val capture = recordingJob
            recording = false
            publish(token, SpeechState.Processing)
            scope.launch(start = CoroutineStart.LAZY) {
                runCatching { record.stop() }
                capture?.join()
                runCatching { record.release() }
                synchronized(lock) {
                    if (audioRecord === record) audioRecord = null
                    recordingJob = null
                    activeEngine = null
                    activeCollector = null
                }
                recognize(token, engine, collector.toFloatArray())
            }.also { processingJob = it }
        }
        job.start()
    }

    private fun recognize(token: Long, engine: OfflineRecognizer, samples: FloatArray) {
        if (!isActive(token)) return
        if (samples.size < MIN_RECORDING_SAMPLES || samples.rootMeanSquare() < MINIMUM_RMS) {
            fail(token, "No clear speech was detected. Move closer to the microphone and try again.")
            return
        }

        try {
            val stream = engine.createStream()
            val text = try {
                stream.acceptWaveform(samples, ParakeetEnglishModel.SAMPLE_RATE)
                engine.decode(stream)
                engine.getResult(stream).text.trim()
            } finally {
                stream.release()
            }
            if (text.isBlank()) {
                fail(token, "Speech was heard but not recognized. Speak clearly and try again.")
            } else {
                val durationMs = samples.size * 1_000L / ParakeetEnglishModel.SAMPLE_RATE
                publish(token, SpeechState.Result(text, durationMs))
            }
        } catch (_: CancellationException) {
            // A canceled session must not publish a stale result.
        } catch (error: Throwable) {
            fail(token, "Offline speech recognition could not complete. Please try again.")
        }
    }

    private fun failCapture(token: Long) {
        val record = synchronized(lock) {
            if (!isActive(token)) return
            recording = false
            audioRecord.also { audioRecord = null }
        }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        fail(token, "The microphone could not capture audio. Check whether another app is using it.")
    }

    private fun createRecognizer(paths: SpeechModelPaths) = OfflineRecognizer(
        assetManager = null,
        config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = ParakeetEnglishModel.SAMPLE_RATE,
                featureDim = 80,
            ),
            modelConfig = OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = paths.encoder,
                    decoder = paths.decoder,
                    joiner = paths.joiner,
                ),
                tokens = paths.tokens,
                numThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
                modelType = "nemo_transducer",
            ),
            decodingMethod = "greedy_search",
            maxActivePaths = 4,
        ),
    )

    private fun publish(token: Long, state: SpeechState) {
        if (isActive(token)) mutableState.value = state
    }

    private fun fail(token: Long, message: String) {
        if (isActive(token)) mutableState.value = SpeechState.Error(message)
    }

    private fun isActive(token: Long): Boolean = synchronized(lock) { token == sessionId }

    private fun preparationErrorMessage(error: Throwable): String = when (error) {
        is SecurityException -> "Microphone permission is required for speech recognition."
        is UnsatisfiedLinkError -> "Offline speech recognition is not compatible with this device."
        else -> if (error.message?.contains("microphone", ignoreCase = true) == true) {
            error.message ?: "The microphone could not start recording."
        } else {
            "Unable to prepare offline English recognition. Connect to the internet once to download the model, then try again."
        }
    }

    private fun FloatArray.rootMeanSquare(): Double {
        if (isEmpty()) return 0.0
        val squareSum = fold(0.0) { total, sample -> total + sample * sample }
        return sqrt(squareSum / size)
    }

    private class FloatChunkCollector {
        private val chunks = mutableListOf<FloatArray>()
        var sampleCount: Int = 0
            private set

        fun add(pcm: ShortArray, count: Int) {
            val samples = FloatArray(count) { index -> pcm[index] / 32_768f }
            chunks += samples
            sampleCount += count
        }

        fun toFloatArray(): FloatArray {
            val result = FloatArray(sampleCount)
            var offset = 0
            chunks.forEach { chunk ->
                chunk.copyInto(result, destinationOffset = offset)
                offset += chunk.size
            }
            return result
        }
    }

    private companion object {
        const val RECORDING_BUFFER_BYTES = 32 * 1024
        const val MAX_RECORDING_SECONDS = 90
        const val MAX_RECORDING_SAMPLES = ParakeetEnglishModel.SAMPLE_RATE * MAX_RECORDING_SECONDS
        const val MIN_RECORDING_SAMPLES = ParakeetEnglishModel.SAMPLE_RATE / 2
        const val MINIMUM_RMS = 0.0025
    }
}
