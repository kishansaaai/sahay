package com.sahay.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.state.Screen
import com.sahay.app.ui.LanguageChips
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr

/**
 * Settings (issue B13): language, model status, export/import of the JSON snapshot and one-tap delete (PRD section 14).
 * Export and import go through the clipboard or the system share sheet, so the app still needs no network permission.
 */
@Composable
fun SettingsScreen(state: AppState) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Resolved here because sr() is @Composable and cannot be called from the button callbacks below.
    val copiedMessage = sr(R.string.settings_export_copied)
    val shareTitle = sr(R.string.settings_share_title)
    val emptyMessage = sr(R.string.settings_import_empty)
    val okMessage = sr(R.string.settings_import_ok)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        SectionCard(title = sr(R.string.language)) {
            LanguageChips(state)
        }

        SectionCard(title = sr(R.string.settings_model)) {
            Text(
                text = sr(R.string.settings_model_file, sr(if (state.modelPresent) R.string.settings_present else R.string.settings_absent)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = sr(R.string.settings_runtime, sr(if (com.sahay.app.llm.SahayLlm.runtimeAvailable) R.string.settings_present else R.string.settings_absent)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = sr(R.string.settings_model_note), style = MaterialTheme.typography.bodySmall)
        }

        SectionCard(title = sr(R.string.settings_data)) {
            Text(
                text = sr(R.string.settings_data_summary, state.exportSummary()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val json = state.exportJson()
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Sahay snapshot", json))
                        message = copiedMessage
                    },
                ) { Text(sr(R.string.settings_copy)) }
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Sahay snapshot")
                            putExtra(Intent.EXTRA_TEXT, state.exportJson())
                        }
                        val chooser = Intent.createChooser(send, shareTitle)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    },
                ) { Text(sr(R.string.settings_share)) }
            }
            OutlinedButton(
                onClick = {
                    val text = clipboard?.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                    if (text.isNullOrBlank()) {
                        message = emptyMessage
                    } else {
                        val error = state.importSnapshot(text)
                        message = if (error == null) okMessage
                        else context.getString(R.string.settings_import_failed, error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(sr(R.string.settings_import)) }
            message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }

        SectionCard(title = sr(R.string.settings_delete)) {
            Text(text = sr(R.string.settings_delete_body), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(sr(R.string.settings_delete))
            }
        }

        OutlinedButton(onClick = { state.go(Screen.Device) }, modifier = Modifier.fillMaxWidth()) {
            Text(sr(R.string.settings_device), fontWeight = FontWeight.SemiBold)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(sr(R.string.settings_delete_confirm)) },
            text = { Text(sr(R.string.settings_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    state.deleteAllData()
                }) { Text(sr(R.string.settings_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(sr(R.string.cancel)) }
            },
        )
    }
}
