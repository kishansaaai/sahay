package com.sahay.app.ui.screens

import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import com.sahay.app.state.HeatRow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.media.MediaInput
import com.sahay.app.state.AppState
import com.sahay.app.ui.sr
import com.sahay.app.ui.MasteryBar
import com.sahay.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareScreen(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf(emptyList<String>()) }
    var page by remember { mutableStateOf(0) }
    var reading by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    var teacherText by remember { mutableStateOf("") }
    var classRows by remember { mutableStateOf(emptyList<HeatRow>()) }
    var collector by remember { mutableStateOf(PlanSharing.Collector()) }
    var progress by remember { mutableStateOf("") }
    var incoming by remember { mutableStateOf<Snapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pseudonym by remember { mutableStateOf("") }
    val heatmap = remember { ClassHeatmap() }
    var rows by remember { mutableStateOf(emptyList<ClassMastery>()) }
    val qr = remember(pages, page) { pages.getOrNull(page)?.let(MediaInput::qrImage) }
    fun read(uri: Uri) {
        if (reading) return
        reading = true
        scope.launch {
            try {
                val encoded = withContext(Dispatchers.IO) {
                    val bitmap = MediaInput.bitmap(context, uri)
                    try { MediaInput.qr(bitmap) } finally { bitmap.recycle() }
                }
                incoming = withContext(Dispatchers.Default) {
                    if (encoded.trim().startsWith(ShareCode.PREFIX)) ShareCode.decode(encoded) else collector.add(encoded)
                }
                progress = "${collector.received} / ${collector.total}"
                error = null
            } catch (e: Exception) { error = e.message ?: "QR could not be read" }
            finally { reading = false }
        }
    }
    val uri = remember { MediaInput.cameraUri(context) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) read(uri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) read(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(sr(R.string.share_title), style = MaterialTheme.typography.headlineSmall)
        Text(sr(R.string.share_notice))
        Button(enabled = state.profile != null, onClick = {
            val snapshot = state.snapshot()
            scope.launch {
                try { pages = withContext(Dispatchers.Default) { PlanSharing.encode(snapshot) }; page = 0; error = null }
                catch (e: Exception) { error = e.message }
            }
        }) { Text(sr(R.string.share_create)) }
        qr?.let {
            Image(it.asImageBitmap(), contentDescription = sr(R.string.share_page, page + 1, pages.size), modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            Text(sr(R.string.share_page, page + 1, pages.size))
            OutlinedButton(onClick = { page-- }, enabled = page > 0) { Text(sr(R.string.back)) }
            OutlinedButton(onClick = { page++ }, enabled = page < pages.lastIndex) { Text(sr(R.string.next)) }
        }
        OutlinedButton(enabled = state.profile != null, onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Sahay share code", ShareCode.encode(state.snapshot())))
        }) { Text(sr(R.string.share_copy)) }
        OutlinedTextField(value = pasted, onValueChange = { pasted = it.take(3_000_000) }, label = { Text(sr(R.string.share_import_hint)) }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
        OutlinedButton(enabled = pasted.isNotBlank() && !reading, onClick = {
            reading = true
            scope.launch {
                try {
                    incoming = withContext(Dispatchers.Default) {
                        if (pasted.trim().startsWith(ShareCode.PREFIX)) ShareCode.decode(pasted)
                        else if (pasted.trim().startsWith("{")) Snapshot.fromJson(pasted)
                        else collector.add(pasted.trim())
                    }
                    progress = "${collector.received} / ${collector.total}"; error = null
                } catch (e: Exception) { error = e.message }
                finally { reading = false }
            }
        }) { Text(sr(R.string.share_import)) }
        HorizontalDivider()
        Button(enabled = !reading, onClick = { camera.launch(uri) }) { Text(sr(R.string.share_scan)) }
        OutlinedButton(enabled = !reading, onClick = { picker.launch("image/*") }) { Text(sr(R.string.scan_image)) }
        Text(progress)
        OutlinedButton(enabled = !reading, onClick = { collector = PlanSharing.Collector(); incoming = null; progress = ""; error = null }) { Text(sr(R.string.share_reset)) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        incoming?.let { snapshot ->
            Text(sr(R.string.share_ready))
            // Replacing personal data is an explicit action, separate from scanning or teacher aggregation.
            Button(onClick = { state.importSnapshot(snapshot.toJson()) { error = it } }) { Text(sr(R.string.share_replace)) }
            OutlinedTextField(value = pseudonym, onValueChange = { pseudonym = it.take(80) }, label = { Text(sr(R.string.share_learner)) })
            Button(enabled = pseudonym.isNotBlank(), onClick = {
                try {
                    val domain = snapshot.profile?.domain ?: throw IllegalArgumentException("snapshot has no profile")
                    heatmap.merge(pseudonym, snapshot, state.contentFor(domain)); rows = heatmap.rows(); error = null
                } catch (e: Exception) { error = e.message }
            }) { Text(sr(R.string.share_merge)) }
        }
        OutlinedTextField(value = teacherText, onValueChange = { teacherText = it.take(3_000_000) }, label = { Text(sr(R.string.share_teacher_note)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedButton(enabled = teacherText.isNotBlank(), onClick = {
            val (result, failure) = state.classHeatmap(teacherText.lines().map { it.trim() }.filter { it.isNotBlank() })
            classRows = result; error = failure
        }) { Text(sr(R.string.share_teacher_title)) }
        classRows.forEach { row ->
            val label = "${row.name}: ${AppState.percent(row.meanMastery)}% (${row.students})"
            Text(label); MasteryBar(row.meanMastery, label = label)
        }
        if (rows.isNotEmpty()) {
            Text(sr(R.string.share_heatmap), style = MaterialTheme.typography.titleMedium)
            Text(sr(R.string.share_class_note))
            rows.forEach { row ->
                Text(row.learnerId)
                row.values.forEach { (id, value) ->
                    val label = "${state.contentFor(row.domain).graph.skills[id]?.name ?: id}: ${AppState.percent(value)}%"
                    Text(label); MasteryBar(value, label = label)
                }
            }
            OutlinedButton(onClick = { heatmap.clear(); rows = emptyList() }) { Text(sr(R.string.share_clear)) }
        }
    }
}
