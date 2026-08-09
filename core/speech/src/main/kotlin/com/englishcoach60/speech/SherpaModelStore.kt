package com.englishcoach60.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal data class SpeechModelFile(
    val name: String,
    val byteCount: Long,
    val sha256: String,
)

internal object ParakeetEnglishModel {
    const val ID = "nemo-parakeet-tdt-0.6b-v2-int8-b9bd3269"
    const val SAMPLE_RATE = 16_000
    private const val REVISION = "b9bd32696df49cbb1e7e499238a22804c82f9ef7"
    const val BASE_URL =
        "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/$REVISION"

    val files = listOf(
        SpeechModelFile(
            name = "encoder.int8.onnx",
            byteCount = 652_184_296,
            sha256 = "a32b12d17bbbc309d0686fbbcc2987b5e9b8333a7da83fa6b089f0a2acd651ab",
        ),
        SpeechModelFile(
            name = "decoder.int8.onnx",
            byteCount = 7_257_753,
            sha256 = "b6bb64963457237b900e496ee9994b59294526439fbcc1fecf705b31a15c6b4e",
        ),
        SpeechModelFile(
            name = "joiner.int8.onnx",
            byteCount = 1_739_080,
            sha256 = "7946164367946e7f9f29a122407c3252b680dbae9a51343eb2488d057c3c43d2",
        ),
        SpeechModelFile(
            name = "tokens.txt",
            byteCount = 9_384,
            sha256 = "ec182b70dd42113aff6c5372c75cac58c952443eb22322f57bbd7f53977d497d",
        ),
    )

    val totalBytes: Long = files.sumOf(SpeechModelFile::byteCount)
}

internal data class SpeechModelPaths(
    val encoder: String,
    val decoder: String,
    val joiner: String,
    val tokens: String,
)

internal class SherpaModelStore(context: Context) {
    private val modelDirectory = File(
        context.applicationContext.noBackupFilesDir,
        "offline-speech/${ParakeetEnglishModel.ID}",
    )
    private val readyMarker = File(modelDirectory, ".ready")

    suspend fun ensureReady(onProgress: (Int) -> Unit): SpeechModelPaths = withContext(Dispatchers.IO) {
        check(modelDirectory.exists() || modelDirectory.mkdirs()) {
            "The offline speech model directory could not be created."
        }

        if (!isReady()) {
            readyMarker.delete()
            ParakeetEnglishModel.files.forEach { modelFile ->
                ensureFile(modelFile, onProgress)
            }
            check(ParakeetEnglishModel.files.all(::isComplete)) {
                "The offline speech model is incomplete. Connect to the internet and try again."
            }
            readyMarker.writeText(ParakeetEnglishModel.ID)
        }
        onProgress(100)
        paths()
    }

    private fun isReady(): Boolean =
        readyMarker.readTextOrNull() == ParakeetEnglishModel.ID &&
            ParakeetEnglishModel.files.all(::isComplete)

    private suspend fun ensureFile(modelFile: SpeechModelFile, onProgress: (Int) -> Unit) {
        val target = File(modelDirectory, modelFile.name)
        if (isComplete(modelFile)) return
        if (target.exists()) target.delete()

        val partial = File(modelDirectory, "${modelFile.name}.partial")
        if (partial.length() > modelFile.byteCount) partial.delete()
        download(modelFile, partial, onProgress)

        check(partial.length() == modelFile.byteCount) {
            "The ${modelFile.name} model file was not downloaded completely."
        }
        check(partial.sha256().equals(modelFile.sha256, ignoreCase = true)) {
            "The ${modelFile.name} model file failed integrity verification."
        }
        try {
            Files.move(
                partial.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                partial.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        reportProgress(onProgress)
    }

    private suspend fun download(
        modelFile: SpeechModelFile,
        partial: File,
        onProgress: (Int) -> Unit,
    ) {
        var downloaded = partial.length()
        val connection = (URL("${ParakeetEnglishModel.BASE_URL}/${modelFile.name}?download=true")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "EnglishCoach60/1.0")
            if (downloaded > 0) setRequestProperty("Range", "bytes=$downloaded-")
        }

        try {
            val response = connection.responseCode
            check(response == HttpURLConnection.HTTP_OK || response == HttpURLConnection.HTTP_PARTIAL) {
                "The offline speech model download failed (HTTP $response)."
            }
            val append = downloaded > 0 && response == HttpURLConnection.HTTP_PARTIAL
            if (!append) downloaded = 0
            FileOutputStream(partial, append).use { output ->
                connection.inputStream.buffered().use { input ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        reportProgress(onProgress)
                    }
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun reportProgress(onProgress: (Int) -> Unit) {
        val downloaded = ParakeetEnglishModel.files.sumOf { modelFile ->
            val target = File(modelDirectory, modelFile.name)
            if (target.length() == modelFile.byteCount) {
                modelFile.byteCount
            } else {
                File(modelDirectory, "${modelFile.name}.partial").length()
                    .coerceAtMost(modelFile.byteCount)
            }
        }
        onProgress(((downloaded * 100) / ParakeetEnglishModel.totalBytes).toInt().coerceIn(0, 100))
    }

    private fun isComplete(modelFile: SpeechModelFile): Boolean =
        File(modelDirectory, modelFile.name).length() == modelFile.byteCount

    private fun paths() = SpeechModelPaths(
        encoder = File(modelDirectory, "encoder.int8.onnx").absolutePath,
        decoder = File(modelDirectory, "decoder.int8.onnx").absolutePath,
        joiner = File(modelDirectory, "joiner.int8.onnx").absolutePath,
        tokens = File(modelDirectory, "tokens.txt").absolutePath,
    )

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 256 * 1024
    }
}
