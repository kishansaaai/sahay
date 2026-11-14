package com.sahay.app.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Voice input prototype (issue B18/X2).
 *
 * Uses Android 12+ on-device recognition only. Older phones retain typed input; a network-capable recognizer is never selected.
 * Whether such a model exists is a property of the phone, so [Dictation.available] and the error codes are surfaced to the
 * UI instead of being hidden. This has never been run on a device; treat every state here as untested.
 */
class SpeechToText(context: Context) {
    private val app: Context = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var lastError: Int? = null

    fun available(): Boolean = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)

    /** Start one utterance. Callbacks arrive on the main thread. */
    fun start(
        languageTag: String,
        onText: (String) -> Unit,
        onError: (Int) -> Unit,
        onListening: (Boolean) -> Unit,
    ) {
        if (!available()) {
            onError(ERROR_UNAVAILABLE)
            return
        }
        recognizer?.destroy()
        val r = if (Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(app).also { created ->
            created.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onListening(true)
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = onListening(false)
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) onText(text)
                }

                override fun onResults(results: Bundle?) {
                    onListening(false)
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull { it.isNotBlank() }
                    if (text == null) onError(ERROR_EMPTY) else onText(text)
                }

                override fun onError(error: Int) {
                    lastError = error
                    onListening(false)
                    onError(error)
                }
            })
            recognizer = created
        } else { onError(ERROR_UNAVAILABLE); return }
        lastError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // The selected service is on-device; retain the hint for its language-pack handling.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        r.startListening(intent)
    }

    fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    fun cancel() {
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    /** Last recognizer error code, for the debug line under the input field. */
    fun lastErrorCode(): Int? = lastError

    companion object {
        /** Not a platform code: no recognizer is installed, so nothing was started. */
        const val ERROR_UNAVAILABLE = -100
        /** Not a platform code: the recognizer returned nothing usable. */
        const val ERROR_EMPTY = -101
    }
}

/**
 * Compose-facing dictation state. Create once with `remember { Dictation(context) }` and call [destroy] from a
 * `DisposableEffect`; [text] holds the last recognised phrase so the caller can put it in the text field.
 */
class Dictation(context: Context) {
    private val speech = SpeechToText(context)

    var available: Boolean by mutableStateOf(speech.available())
        private set
    var listening: Boolean by mutableStateOf(false)
        private set
    var text: String by mutableStateOf("")
        private set
    var errorCode: Int? by mutableStateOf(null)
        private set

    fun start(languageTag: String) {
        if (!available) {
            errorCode = SpeechToText.ERROR_UNAVAILABLE
            return
        }
        errorCode = null
        text = ""
        speech.start(
            languageTag = languageTag,
            onText = { text = it },
            onError = { errorCode = it; listening = false },
            onListening = { listening = it },
        )
    }

    fun stop() {
        speech.stop()
        listening = false
    }

    fun destroy() {
        speech.destroy()
        listening = false
    }
}
