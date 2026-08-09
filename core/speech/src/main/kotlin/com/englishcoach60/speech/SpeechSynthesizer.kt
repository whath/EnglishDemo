package com.englishcoach60.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidSpeechSynthesizer(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var onDone: (() -> Unit)? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) setLocale("en-US")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDone?.invoke()
            }
        })
    }

    fun setLocale(tag: String) {
        if (!ready) return
        val locale = Locale.forLanguageTag(tag)
        tts.language = locale
        val preferred = tts.voices.orEmpty()
            .filter { it.locale.language == locale.language }
            .sortedWith(
                compareByDescending<android.speech.tts.Voice> { it.locale == locale }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality },
            )
            .firstOrNull()
        if (preferred != null) tts.voice = preferred
    }

    fun speak(text: String, rate: Float = 1f) {
        if (!ready || text.isBlank()) return
        tts.stop()
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
