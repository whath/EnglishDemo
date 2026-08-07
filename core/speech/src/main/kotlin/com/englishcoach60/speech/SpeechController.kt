package com.englishcoach60.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Listening : SpeechState
    data object Processing : SpeechState
    data class Result(val text: String, val durationMs: Long) : SpeechState
    data class Error(val message: String) : SpeechState
    data object Unavailable : SpeechState
}

class AndroidSpeechController(private val context: Context) : RecognitionListener {
    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = mutableState
    private var recognizer: SpeechRecognizer? = null
    private var startedAt = 0L

    fun start() {
        if (mutableState.value == SpeechState.Listening || mutableState.value == SpeechState.Processing) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            mutableState.value = SpeechState.Unavailable
            return
        }
        destroyRecognizer()
        recognizer = if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(this)
        startedAt = System.currentTimeMillis()
        mutableState.value = SpeechState.Listening
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    fun stop() {
        if (mutableState.value == SpeechState.Listening) {
            mutableState.value = SpeechState.Processing
            recognizer?.stopListening()
        }
    }

    fun cancel() {
        recognizer?.cancel()
        destroyRecognizer()
        mutableState.value = SpeechState.Idle
    }

    fun consumeResult() { mutableState.value = SpeechState.Idle }
    fun destroy() { destroyRecognizer(); mutableState.value = SpeechState.Idle }
    private fun destroyRecognizer() { recognizer?.destroy(); recognizer = null }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { mutableState.value = SpeechState.Processing }
    override fun onError(error: Int) {
        mutableState.value = SpeechState.Error("Speech wasn't recognized. You can try again or use the keyboard.")
        destroyRecognizer()
    }
    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        mutableState.value = if (text.isBlank()) SpeechState.Error("No speech detected.") else SpeechState.Result(text, System.currentTimeMillis() - startedAt)
        destroyRecognizer()
    }
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

class AndroidSpeechSynthesizer(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var onDone: (() -> Unit)? = null
    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) setLocale("en-US")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { onDone?.invoke() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { onDone?.invoke() }
        })
    }
    fun setLocale(tag: String) {
        if (!ready) return
        val locale = Locale.forLanguageTag(tag)
        tts.language = locale
        val preferred = tts.voices.orEmpty()
            .filter { it.locale.language == locale.language }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale == locale }.thenBy { it.isNetworkConnectionRequired }.thenByDescending { it.quality })
            .firstOrNull()
        if (preferred != null) tts.voice = preferred
    }
    fun speak(text: String, rate: Float = 1f) {
        if (!ready || text.isBlank()) return
        tts.stop(); tts.setSpeechRate(rate); tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }
    fun stop() { tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
