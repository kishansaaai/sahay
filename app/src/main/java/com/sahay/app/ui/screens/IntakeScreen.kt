package com.sahay.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.ChipRow
import com.sahay.app.ui.ProgressDots
import com.sahay.app.ui.sr

/**
 * Intake chat (issue B05, C1). A chat-styled stepper: free text for the goal, quick-reply chips for every other field.
 * A free-text goal that cannot be mapped to a domain falls back to the domain chips instead of being guessed.
 */
@Composable
fun IntakeScreen(state: AppState) {
    var step by remember { mutableStateOf(0) }
    var goal by remember { mutableStateOf("") }
    var domainId by remember { mutableStateOf("dsa") }
    var level by remember { mutableStateOf("beginner") }
    var hours by remember { mutableStateOf(6.0) }
    var weeks by remember { mutableStateOf(8) }
    var interests by remember { mutableStateOf(setOf<String>()) }
    var parseError by remember { mutableStateOf(false) }

    val totalSteps = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = sr(R.string.intake_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = sr(R.string.intake_step, step + 1, totalSteps), style = MaterialTheme.typography.labelLarge)
        ProgressDots(total = totalSteps, current = step)

        when (step) {
            0 -> {
                AssistantBubble(sr(R.string.intake_goal_prompt))
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it; parseError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(sr(R.string.intake_goal_placeholder)) },
                    maxLines = 3,
                )
                if (parseError) Text(text = sr(R.string.intake_parse_failed), color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = {
                        val found = state.domainFromGoal(goal)
                        if (found == null) parseError = true else {
                            domainId = found
                            step = 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(sr(R.string.intake_use_goal)) }
                Text(text = sr(R.string.intake_domain_prompt), style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    ChipRow(sr(R.string.domain_dsa), domainId == "dsa") { domainId = "dsa"; step = 1 }
                    ChipRow(sr(R.string.domain_mleng), domainId == "mleng") { domainId = "mleng"; step = 1 }
                }
            }

            1 -> {
                AssistantBubble(sr(R.string.intake_level_prompt))
                val levels = listOf(
                    "beginner" to sr(R.string.level_beginner),
                    "intermediate" to sr(R.string.level_intermediate),
                    "advanced" to sr(R.string.level_advanced),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    levels.forEach { (code, label) -> ChipRow(label, level == code) { level = code; step = 2 } }
                }
            }

            2 -> {
                AssistantBubble(sr(R.string.intake_hours_prompt))
                val options = listOf(3, 6, 10, 14)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    options.forEach { value -> ChipRow(sr(R.string.hours_option, value), hours == value.toDouble()) { hours = value.toDouble(); step = 3 } }
                }
            }

            3 -> {
                AssistantBubble(sr(R.string.intake_deadline_prompt))
                val options = listOf(4, 6, 10, 16)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    options.forEach { value -> ChipRow(sr(R.string.weeks_option, value), weeks == value) { weeks = value; step = 4 } }
                }
            }

            4 -> {
                AssistantBubble(sr(R.string.intake_interests_prompt))
                val options = listOf(
                    "cricket" to sr(R.string.interest_cricket),
                    "movies" to sr(R.string.interest_movies),
                    "music" to sr(R.string.interest_music),
                    "cooking" to sr(R.string.interest_cooking),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    options.forEach { (code, label) ->
                        ChipRow(label, code in interests) {
                            interests = if (code in interests) interests - code else interests + code
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedButton(onClick = { step = 5 }) { Text(sr(R.string.continue_label)) }
                    Text(text = sr(R.string.interest_none), style = MaterialTheme.typography.bodySmall)
                }
            }

            else -> {
                AssistantBubble(sr(R.string.intake_review_prompt))
                val fallbackGoal = goalLabel(domainId)
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReviewLine(sr(R.string.intake_goal_label), if (goal.isBlank()) fallbackGoal else goal)
                        ReviewLine(sr(R.string.intake_level_label), levelLabel(level))
                        ReviewLine(sr(R.string.intake_hours_label), sr(R.string.hours_option, hours.toInt()))
                        ReviewLine(sr(R.string.intake_deadline_label), sr(R.string.weeks_option, weeks))
                        ReviewLine(sr(R.string.intake_interests_label), interests.joinToString(", ").ifBlank { sr(R.string.why_this_none) })
                    }
                }
                Button(
                    onClick = {
                        state.createProfile(
                            goal = goal.ifBlank { fallbackGoal },
                            domainId = domainId,
                            level = level,
                            weeklyHours = hours,
                            weeks = weeks,
                            interests = interests.toList(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(sr(R.string.intake_finish)) }
            }
        }
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Text(text = "$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun goalLabel(domainId: String): String =
    if (domainId == "dsa") sr(R.string.domain_dsa) else sr(R.string.domain_mleng)

@Composable
private fun levelLabel(code: String): String = when (code) {
    "beginner" -> sr(R.string.level_beginner)
    "advanced" -> sr(R.string.level_advanced)
    else -> sr(R.string.level_intermediate)
}

@Composable
private fun AssistantBubble(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
