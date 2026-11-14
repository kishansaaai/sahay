package com.sahay.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sahay.app.R
import com.sahay.app.scan.OcrEngine
import com.sahay.app.state.AppState
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr
import kotlinx.coroutines.launch
import java.io.File

/**
 * Scan (issues B12, I4, I8): a photo or picked image is read with on-device OCR, the learner edits the text, then either
 * builds an editable topic list from a syllabus (I4) or gets a short summary and up to five questions from a page (I8).
 * Typing or pasting text works with no camera and no OCR at all. Photos are deleted as soon as they have been read.
 *
 * Not run on a phone in this environment: OCR accuracy, the 80% syllabus coverage target and the 60-second page-to-quiz
 * target are all unmeasured.
 */
@Composable
fun ScanScreen(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val hindi = state.language == "hi"

    // Resolved here because sr() is @Composable and cannot be called from the callbacks below.
    val cameraMissing = sr(R.string.scan_camera_missing)
    val readFailed = { reason: String -> context.getString(R.string.scan_read_failed, reason) }

    fun readImage(uri: Uri, deleteAfter: File?) {
        reading = true
        scope.launch {
            val result = OcrEngine.read(context, uri, hindi)
            result.onSuccess { state.addScannedText(it) }
                .onFailure { state.reportScanProblem(readFailed(it.message ?: it.javaClass.simpleName)) }
            deleteAfter?.delete()
            reading = false
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPhoto
        pendingPhoto = null
        if (saved && file != null) {
            readImage(FileProvider.getUriForFile(context, context.packageName + ".scanfiles", file), file)
        } else {
            file?.delete()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) readImage(uri, null)
    }

    fun takePhoto() {
        val file = newPhotoFile(context)
        pendingPhoto = file
        val uri = FileProvider.getUriForFile(context, context.packageName + ".scanfiles", file)
        try {
            camera.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            pendingPhoto = null
            file.delete()
            state.reportScanProblem(cameraMissing)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = sr(R.string.scan_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = sr(R.string.scan_intro), style = MaterialTheme.typography.bodyMedium)
        Text(text = sr(R.string.scan_privacy), style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { takePhoto() }, enabled = !reading && !state.scanBusy) { Text(sr(R.string.scan_take_photo)) }
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !reading && !state.scanBusy) {
                Text(sr(R.string.scan_pick_image))
            }
        }
        if (reading) {
            Text(text = sr(R.string.scan_reading), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = state.scanText,
            onValueChange = { state.setScanText(it) },
            label = { Text(sr(R.string.scan_text_label)) },
            minLines = 6,
            maxLines = 14,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { state.compileSyllabus() }, enabled = !state.scanBusy && !reading) { Text(sr(R.string.scan_build_topics)) }
            OutlinedButton(onClick = { state.makeScanQuiz() }, enabled = !state.scanBusy && !reading) { Text(sr(R.string.scan_make_quiz)) }
        }
        if (state.scanBusy) {
            Text(text = sr(R.string.scan_working), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.scanMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

        DraftSection(state)
        QuizSection(state)

        TextButton(onClick = { state.clearScan() }) { Text(sr(R.string.scan_clear)) }
    }
}

@Composable
private fun DraftSection(state: AppState) {
    val draft = state.scanDraft ?: return
    val errors = draft.errors()
    SectionCard(title = sr(R.string.scan_topics_title, draft.included().size, "%.1f".format(draft.totalHours()))) {
        if (state.scanDraftTemplate) Text(text = sr(R.string.scan_template_note), style = MaterialTheme.typography.bodySmall)
        draft.skills.forEach { skill ->
            val include = sr(R.string.scan_topic_include, skill.name)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = skill.included,
                        onCheckedChange = { state.editDraft { d -> d.toggle(skill.id) } },
                        modifier = Modifier.semantics { contentDescription = include },
                    )
                    OutlinedTextField(
                        value = skill.name,
                        onValueChange = { v -> state.editDraft { d -> d.rename(skill.id, v) } },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val less = sr(R.string.scan_hours_less, skill.name)
                    val more = sr(R.string.scan_hours_more, skill.name)
                    val remove = sr(R.string.scan_remove, skill.name)
                    TextButton(
                        onClick = { state.editDraft { d -> d.withHours(skill.id, skill.hours - 0.5) } },
                        modifier = Modifier.semantics { contentDescription = less },
                    ) { Text("−") }
                    Text(text = sr(R.string.scan_hours, "%.1f".format(skill.hours)), style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = { state.editDraft { d -> d.withHours(skill.id, skill.hours + 0.5) } },
                        modifier = Modifier.semantics { contentDescription = more },
                    ) { Text("+") }
                    TextButton(
                        onClick = { state.editDraft { d -> d.remove(skill.id) } },
                        modifier = Modifier.semantics { contentDescription = remove },
                    ) { Text("✕") }
                }
            }
        }
        if (errors.isNotEmpty()) {
            Text(
                text = sr(R.string.scan_errors, errors.joinToString("; ")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = { state.focusPlanOnScan() }, enabled = errors.isEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(sr(R.string.scan_use_plan))
        }
    }
}

@Composable
private fun QuizSection(state: AppState) {
    if (state.scanSummary.isNotBlank()) {
        SectionCard(title = sr(R.string.scan_summary_title)) {
            Text(text = state.scanSummary, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (state.scanQuestions.isEmpty()) return
    val picked = remember(state.scanQuestions.firstOrNull()?.stem) { mutableStateMapOf<Int, Int>() }
    SectionCard(title = sr(R.string.scan_quiz_title)) {
        state.scanQuestions.forEachIndexed { qi, q ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "${qi + 1}. ${q.stem}", style = MaterialTheme.typography.titleSmall)
                val choice = picked[qi]
                q.options.forEachIndexed { oi, text ->
                    OutlinedButton(onClick = { if (choice == null) picked[qi] = oi }, enabled = choice == null, modifier = Modifier.fillMaxWidth()) {
                        Text(text)
                    }
                }
                if (choice != null) {
                    Text(
                        text = if (choice == q.answer) sr(R.string.scan_correct) else sr(R.string.scan_wrong, q.options[q.answer]),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    q.misconceptions.getOrNull(choice)?.let {
                        if (choice != q.answer) Text(text = sr(R.string.scan_misconception, it), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** A fresh file in the app's private cache; the FileProvider in the manifest hands it to the camera app. */
private fun newPhotoFile(context: Context): File {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    return File(dir, "scan_${System.currentTimeMillis()}.jpg")
}
