package com.sahay.app.media

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Never falls back to a recognizer or TTS voice that may use the network. Must be used on the main thread. */
class OfflineVoice(private val context: Context, private val onText: (String) -> Unit, private val onError: (String) -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    init {
        tts = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }
    fun listen(language: String) {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            onError("Offline speech recognition is unavailable on this phone. Type your question instead.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
                }
                override fun onError(error: Int) { onError("Offline recognition unavailable (code $error). Check that the language pack is installed.") }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(language).toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        }
    }
    fun speak(text: String, language: String) {
        val engine = tts
        if (!ttsReady || engine == null) { onError("Offline voice is not ready."); return }
        val locale = Locale.forLanguageTag(language)
        val voice = engine.voices?.firstOrNull { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
        if (voice == null) { onError("No downloaded offline voice for this language."); return }
        engine.voice = voice
        engine.speak(text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, null, "sahay-reply")
    }
    fun stop() { recognizer?.cancel(); tts?.stop() }
    fun close() { recognizer?.destroy(); tts?.shutdown(); recognizer = null; tts = null }
}
