package com.sahay.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sahay.app.R
import com.sahay.app.media.MediaInput
import com.sahay.app.state.AppState
import com.sahay.app.ui.sr
import com.sahay.engine.LlmResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Both OCR models are bundled; capture is delegated to the system camera, with no camera or network permission. */
@Composable
fun ScanScreen(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<LlmResult?>(null) }
    var choice by remember { mutableStateOf<Int?>(null) }
    var revealed by remember { mutableStateOf(false) }
    val recognizer = remember(state.language) {
        if (state.language in setOf("hi", "mr")) TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    var active by remember { mutableStateOf(true) }
    DisposableEffect(recognizer) { active = true; onDispose { active = false; recognizer.close() } }
    fun read(uri: Uri) {
        busy = true; error = null; result = null
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { MediaInput.bitmap(context, uri) }
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { if (active) { text = it.text.take(6000); busy = false } }
                    .addOnFailureListener { if (active) { error = it.message; busy = false } }
                    .addOnCompleteListener { bitmap.recycle() }
            } catch (e: Exception) { error = e.message; busy = false }
        }
    }
    val cameraUri = remember { MediaInput.cameraUri(context) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) read(cameraUri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) read(uri) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(sr(R.string.scan_title), style = MaterialTheme.typography.headlineSmall)
        Text(sr(R.string.scan_note))
        Button(onClick = { camera.launch(cameraUri) }, enabled = !busy) { Text(sr(R.string.scan_camera)) }
        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text(sr(R.string.scan_image)) }
        OutlinedTextField(value = text, onValueChange = { text = it.take(6000); result = null }, label = { Text(sr(R.string.scan_edit)) },
            modifier = Modifier.fillMaxWidth(), minLines = 6, enabled = !busy)
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        for ((task, label) in listOf("syllabus_compile" to R.string.scan_syllabus, "draft_question" to R.string.scan_quiz)) {
            Button(enabled = text.isNotBlank() && !busy, onClick = {
                busy = true; result = null; choice = null; revealed = false
                state.runScannedTask(task, text) { result = it; busy = false }
            }, modifier = Modifier.fillMaxWidth()) { Text(sr(label)) }
        }
        result?.let { r ->
            Text(sr(R.string.scan_draft))
            val output = r.output
            if (output == null) Text(sr(R.string.scan_unavailable))
            else if (r.task == "draft_question") {
                Text(output["stem"].toString(), style = MaterialTheme.typography.titleMedium)
                val options = output["options"] as List<Map<String, Any?>>
                options.forEachIndexed { index, option ->
                    FilterChip(selected = choice == index, onClick = { choice = index }, label = { Text(option["text"].toString()) })
                }
                Button(enabled = choice != null, onClick = { revealed = true }) { Text(sr(R.string.diag_submit)) }
                if (revealed) Text(sr(R.string.diag_answer_reveal, options[(output["answer"] as Number).toInt()]["text"].toString()))
            } else {
                val skills = output["skills"] as List<Map<String, Any?>>
                skills.forEach { Text(sr(R.string.scan_skill_estimate, it["name"].toString(), it["hours"].toString())) }
                Text(sr(R.string.scan_dependencies, (output["prerequisites"] as List<*>).size))
            }
            if (r.source == "fallback") Text(sr(R.string.tutor_template))
        }
    }
}
