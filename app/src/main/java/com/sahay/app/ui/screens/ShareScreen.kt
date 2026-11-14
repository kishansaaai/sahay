package com.sahay.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.MasteryBar
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr
import com.sahay.engine.ShareCode

/**
 * Offline roadmap sharing (issue B17/X3): the whole snapshot is compressed into one text code, drawn as a QR symbol with
 * ZXing, and importable from pasted text or a screenshot. A teacher phone can merge up to five student codes into a
 * per-skill heatmap, with each student's mastery replayed locally.
 *
 * Nothing here has been run on a phone: the QR is drawn by a library but never scanned, and the heatmap has only been
 * exercised by unit tests on synthetic snapshots.
 */
@Composable
fun ShareScreen(state: AppState) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val shareCode = remember(state.revision) {
        if (state.plan == null) null else runCatching { ShareCode.encode(state.snapshot()) }.getOrNull()
    }
    // Resolved here because sr() is @Composable and cannot be called from the callbacks below.
    val emptyClipboard = sr(R.string.settings_import_empty)
    val importOkMessage = sr(R.string.share_import_ok)
    val scanFailedMessage = sr(R.string.share_image_failed)
    val teacherEmptyMessage = sr(R.string.share_teacher_empty)
    val fits = shareCode != null && ShareCode.qrFits(shareCode)
    var copied by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var teacherText by remember { mutableStateOf("") }
    var heat by remember { mutableStateOf<List<AppState.HeatRow>>(emptyList()) }
    var teacherMessage by remember { mutableStateOf<String?>(null) }
    var teacherCount by remember { mutableStateOf(0) }

    fun takeText(text: String?) {
        if (text.isNullOrBlank()) {
            importMessage = emptyClipboard
            return
        }
        val error = state.importSnapshot(text)
        importMessage = if (error == null) importOkMessage else context.getString(R.string.share_import_failed, error)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val found = decodeQr(context, uri)
            if (found == null) scanMessage = scanFailedMessage else { scanMessage = null; takeText(found) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.share_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = sr(R.string.share_intro), style = MaterialTheme.typography.bodyMedium)

        SectionCard(title = sr(R.string.share_qr_title)) {
            when {
                shareCode == null -> Text(sr(R.string.share_qr_missing), style = MaterialTheme.typography.bodyMedium)
                !fits -> Text(sr(R.string.share_qr_too_big), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    QrImage(shareCode)
                    Text(
                        text = sr(R.string.share_code_label),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Sahay share code", shareCode))
                            copied = true
                        }) { Text(sr(R.string.share_copy)) }
                    }
                    if (copied) Text(text = sr(R.string.share_copied), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SectionCard(title = sr(R.string.share_import_title)) {
            Text(text = sr(R.string.share_import_note), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(sr(R.string.share_import_hint)) },
                minLines = 3,
                maxLines = 6,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { takeText(importText) }, enabled = importText.isNotBlank()) {
                    Text(sr(R.string.share_import))
                }
                OutlinedButton(onClick = { picker.launch("image/*") }) { Text(sr(R.string.share_image)) }
            }
            importMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            scanMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }

        SectionCard(title = sr(R.string.share_teacher_title)) {
            Text(text = sr(R.string.share_teacher_note), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = teacherText,
                onValueChange = { teacherText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(sr(R.string.share_teacher_hint)) },
                minLines = 4,
                maxLines = 8,
            )
            Button(
                onClick = {
                    val codes = teacherText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.take(5).toList()
                    if (codes.isEmpty()) {
                        teacherMessage = teacherEmptyMessage
                        heat = emptyList()
                    } else {
                        val (rows, error) = state.classHeatmap(codes)
                        heat = rows
                        teacherCount = codes.size
                        teacherMessage = if (error == null) null else context.getString(R.string.share_import_failed, error)
                    }
                },
                enabled = teacherText.isNotBlank(),
            ) { Text(sr(R.string.share_teacher_build)) }

            if (heat.isNotEmpty()) {
                Text(
                    text = sr(R.string.share_teacher_students, teacherCount),
                    style = MaterialTheme.typography.titleSmall,
                )
                heat.take(30).forEach { row ->
                    Text(
                        text = sr(
                            R.string.share_teacher_row,
                            row.name,
                            AppState.percent(row.meanMastery),
                            row.students,
                            teacherCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    MasteryBar(fraction = row.meanMastery)
                }
            }
            teacherMessage?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun QrImage(text: String) {
    val bitmap = remember(text) { runCatching { renderQr(text, 640) }.getOrNull() }
    if (bitmap == null) {
        Text(text = sr(R.string.share_qr_failed), style = MaterialTheme.typography.bodyMedium)
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = sr(R.string.share_qr_desc),
            modifier = Modifier.size(240.dp),
        )
    }
}

private fun renderQr(text: String, pixels: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, pixels, pixels)
    val width = matrix.width
    val height = matrix.height
    val argb = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            argb[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(argb, 0, width, 0, 0, width, height)
    return bitmap
}

/** Read a QR symbol out of an image the user picked (screenshot or photo). No camera permission is needed. */
private fun decodeQr(context: Context, uri: Uri): String? = runCatching {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val binary = BinaryBitmap(HybridBinarizer(source))
    MultiFormatReader().decode(binary).text
}.getOrNull()
