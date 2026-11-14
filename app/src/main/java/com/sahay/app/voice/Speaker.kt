package com.sahay.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Text-to-speech prototype (issue B18/X2). Uses the phone's installed TTS engine, so it needs no bundled voice data.
 * If no voice exists for the selected language, [languageMissing] is set and the UI says so instead of speaking English
 * with a Hindi label.
 */
class Speaker(context: Context) {
    private val app: Context = context.applicationContext
    private var engine: TextToSpeech? = null
    private var closed = false

    var ready: Boolean by mutableStateOf(false)
        private set
    var languageMissing: Boolean by mutableStateOf(false)
        private set

    init {
        engine = TextToSpeech(app) { status ->
            if (!closed) ready = status == TextToSpeech.SUCCESS
        }
    }

    /** Speak [text] in [languageTag], replacing anything already queued. Returns false when nothing was spoken. */
    fun speak(text: String, languageTag: String): Boolean {
        val tts = engine ?: return false
        if (!ready || text.isBlank()) return false
        val language = Locale.forLanguageTag(languageTag).language
        val offline = tts.voices?.firstOrNull { it.locale.language == language && !it.isNetworkConnectionRequired }
        languageMissing = offline == null
        if (offline == null) return false
        tts.voice = offline
        return tts.speak(text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) == TextToSpeech.SUCCESS
    }

    fun stop() {
        runCatching { engine?.stop() }
    }

    fun shutdown() {
        closed = true
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "sahay"
    }
}
