@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sahay.app.ui.screens

import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr

/**
 * Study and practice (issue B09): one skill, a streamed explanation from `tutor.explain` (template fallback when the model
 * is absent), an analogy toggle, the source note, and a check question that updates mastery and re-plans on a wrong answer.
 */
@Composable
fun StudyScreen(state: AppState) {
    val skillId = state.studySkillId
    if (skillId == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(text = sr(R.string.study_no_skill), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var showAnalogy by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    val content = state.contentFor(state.domain)
    val skill = content.graph.skills[skillId]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (state.studyPractice) sr(R.string.study_practice_title) else sr(R.string.study_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = skill?.name ?: skillId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        SectionCard(title = sr(R.string.study_explanation)) {
            if (state.studyLoading) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text(text = sr(R.string.study_generating), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { state.stopStudy() }) { Text(sr(R.string.study_stop)) }
                }
                if (state.studyText.isNotEmpty()) Text(state.studyText, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(text = state.studyText, style = MaterialTheme.typography.bodyLarge)
                if (state.studyTemplate) {
                    Text(text = sr(R.string.study_template_notice), style = MaterialTheme.typography.bodySmall)
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (state.studyAnalogy.isBlank()) state.requestAnalogy()
                        showAnalogy = !showAnalogy
                    },
                ) { Text(if (showAnalogy) sr(R.string.study_hide_analogy) else sr(R.string.study_show_analogy)) }
                TextButton(onClick = { showSource = !showSource }) { Text(sr(R.string.study_source_note)) }
            }

            if (showAnalogy) {
                Text(
                    text = state.studyAnalogy.ifBlank { sr(R.string.study_generating) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (showSource) {
                Text(text = state.studySource, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.language !in setOf("en", "hi")) Text(sr(R.string.experimental_language), style = MaterialTheme.typography.bodySmall)
        val question = state.studyQuestion
        SectionCard(title = sr(R.string.study_check_question)) {
            if (question == null) {
                Text(text = sr(R.string.study_no_question), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = question.stem, style = MaterialTheme.typography.titleMedium)
                question.options.forEachIndexed { index, option ->
                    val selected = state.checkPendingChoice == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = 48.dp)
                            .selectable(selected = selected, enabled = state.checkFeedback == null, role = Role.RadioButton, onClick = { state.chooseCheckOption(index) }),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(text = option.text, modifier = Modifier.padding(12.dp))
                    }
                }
                val feedback = state.checkFeedback
                if (feedback == null) {
                    Button(
                        onClick = { state.submitCheckAnswer() },
                        enabled = state.checkPendingChoice != null,
                    ) { Text(sr(R.string.diag_submit)) }
                } else {
                    Text(
                        text = if (feedback.correct) sr(R.string.study_answer_correct) else sr(R.string.study_answer_wrong),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!feedback.correct) {
                        Text(text = sr(R.string.diag_answer_reveal, feedback.correctAnswer))
                        feedback.misconception?.let {
                            Text(text = sr(R.string.study_misconception, it), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { state.nextCheckQuestion() }) { Text(sr(R.string.study_retry)) }
                        Button(onClick = { state.go(com.sahay.app.state.Screen.Today) }) { Text(sr(R.string.study_done)) }
                    }
                }
            }
        }

        OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Today) }, modifier = Modifier.fillMaxWidth()) {
            Text(sr(R.string.roadmap_start_today))
        }
    }
}
