package com.sahay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.state.ChatLine
import com.sahay.app.ui.sr

/**
 * Streaming tutor chat (issue B06): tokens are appended as they arrive from `SahayLlm.stream`, a visible Stop button cancels
 * generation, the source note is one tap away, and a missing model is labelled instead of hidden.
 */
@Composable
fun TutorScreen(state: AppState) {
    var input by remember { mutableStateOf("") }
    var showSource by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.chat.size, state.chat.lastOrNull()?.text) {
        if (state.chat.isNotEmpty()) listState.animateScrollToItem(state.chat.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = sr(R.string.tutor_title), style = MaterialTheme.typography.headlineSmall)

        val contextName = state.contextSkillName()
        if (contextName.isNotBlank()) {
            Text(text = sr(R.string.tutor_context, contextName), style = MaterialTheme.typography.bodySmall)
        }
        if (!state.modelPresent) {
            Text(text = sr(R.string.tutor_model_missing), style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showSource = !showSource }) {
                Text(if (showSource) sr(R.string.tutor_hide_source_note) else sr(R.string.tutor_source_note))
            }
        }
        if (showSource) {
            Card {
                Text(text = state.contextSkillNotes(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.chat.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = sr(R.string.tutor_empty), modifier = Modifier.padding(14.dp))
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.chat) { _, line -> ChatBubble(line) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(sr(R.string.tutor_placeholder)) },
                maxLines = 4,
            )
            Button(
                onClick = {
                    state.sendTutor(input)
                    input = ""
                },
                enabled = input.isNotBlank() && !state.tutorStreaming,
            ) { Text(sr(R.string.tutor_send)) }
        }
        if (state.tutorStreaming) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                OutlinedButton(onClick = { state.stopTutor() }) { Text(sr(R.string.tutor_stop)) }
            }
        }
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    val fromMe = line.from == "you"
    Row(modifier = Modifier.fillMaxWidth()) {
        if (fromMe) Spacer(modifier = Modifier.weight(1f))
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(
                containerColor = if (fromMe) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (fromMe) sr(R.string.tutor_you) else sr(R.string.tutor_sahay),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = line.text, style = MaterialTheme.typography.bodyMedium)
                if (line.template) {
                    Text(text = sr(R.string.tutor_template), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (!fromMe) Spacer(modifier = Modifier.weight(1f))
    }
}
