package com.sahay.app.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device OCR (issues B12, I4, I8) with the bundled ML Kit text recognisers. The models ship inside the APK, so no
 * download and no network are used; the manifest still carries no INTERNET permission (see AndroidManifest.xml).
 * The Devanagari recogniser also reads Latin script, so it is used whenever the interface language is Hindi.
 *
 * Never run on a phone in this environment: accuracy on real syllabus photos is the open B12 measurement.
 */
object OcrEngine {
    /** Reads [uri] and returns the recognised text, or the failure. The recogniser is always closed. */
    suspend fun read(context: Context, uri: Uri, hindi: Boolean): Result<String> {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrElse { return Result.failure(it) }
        val recogniser = if (hindi) {
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        } else {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
        return try {
            suspendCancellableCoroutine { cont ->
                recogniser.process(image)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(Result.success(it.text)) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(Result.failure(it)) }
            }
        } finally {
            recogniser.close()
        }
    }
}
