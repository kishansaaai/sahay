package com.sahay.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.sr

/**
 * Diagnostic quiz (issue B07, C3 and I1): confidence slider before every answer, hint ladder button, and a misconception
 * note after a wrong answer. Attempts include confidence, response time and hint level (stored by AppState).
 */
@Composable
fun DiagnosticScreen(state: AppState) {
    val question = state.question
    val feedback = state.diagnosticFeedback

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = sr(R.string.diag_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = sr(R.string.diag_question_n, state.diagnosticAnswerCount + 1) + "  " + sr(R.string.diag_range_note),
            style = MaterialTheme.typography.bodySmall,
        )

        if (feedback != null) {
            DiagnosticResult(state, feedback.correct)
        } else if (question == null) {
            Text(text = sr(R.string.diag_no_question), style = MaterialTheme.typography.bodyLarge)
        } else {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = question.stem, style = MaterialTheme.typography.titleMedium)
                    question.options.forEachIndexed { index, option ->
                        val selected = state.pendingChoice == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { state.chooseOption(index) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Text(text = option.text, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            SectionTitle(sr(R.string.diag_confidence, AppState.percent(state.confidence / 100.0)))
            Slider(
                value = state.confidence.toFloat(),
                onValueChange = { state.setConfidence(it.toDouble()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )

            state.hintLevel?.let { level ->
                Text(text = sr(R.string.diag_hint_level, level), style = MaterialTheme.typography.labelLarge)
            }
            if (state.hintText.isNotEmpty()) {
                Card {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = state.hintText, style = MaterialTheme.typography.bodyMedium)
                        if (state.hintTemplate) Text(text = sr(R.string.diag_template_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.hintLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            OutlinedButton(
                onClick = { state.requestHint() },
                enabled = (state.hintLevel ?: -1) < 3 && !state.hintLoading,
            ) {
                Text(if ((state.hintLevel ?: -1) < 3) sr(R.string.diag_hint) else sr(R.string.diag_hint_disabled))
            }

            Button(
                onClick = { state.submitDiagnosticAnswer() },
                enabled = state.pendingChoice != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(sr(R.string.diag_submit)) }
        }
    }
}

@Composable
private fun DiagnosticResult(state: AppState, correct: Boolean) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (correct) sr(R.string.diag_correct) else sr(R.string.diag_wrong),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val feedback = state.diagnosticFeedback
            if (feedback != null) {
                if (!correct) {
                    Text(text = sr(R.string.diag_answer_reveal, feedback.correctAnswer))
                    feedback.misconception?.let {
                        Text(text = sr(R.string.diag_misconception, it), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(text = feedback.skillName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val finished = state.diagnostic?.done() == true
    Button(onClick = { state.nextDiagnosticQuestion() }, modifier = Modifier.fillMaxWidth()) {
        Text(if (finished) sr(R.string.diag_finish) else sr(R.string.diag_next))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}
