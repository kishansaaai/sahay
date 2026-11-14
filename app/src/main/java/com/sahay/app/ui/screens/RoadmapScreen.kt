package com.sahay.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.sahay.engine.PlanItem

/**
 * Roadmap screen (issue B08): week cards with learn, practice, revise and repair items, a one-sentence change note, and a
 * "why this" dialog for every item that shows the planner reason, prerequisites and reference note.
 */
@Composable
fun RoadmapScreen(state: AppState) {
    val plan = state.plan
    var why by remember { mutableStateOf<PlanItem?>(null) }
    val whyLabel = sr(R.string.why_this)

    if (plan == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = sr(R.string.roadmap_title), style = MaterialTheme.typography.headlineSmall)
            Text(text = sr(R.string.roadmap_empty), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { state.startIntake() }) { Text(sr(R.string.welcome_start)) }
        }
        return
    }

    val content = state.contentFor(state.domain)
    val weeks = remember(plan, state.revision) { plan.weeks(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.roadmap_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = sr(R.string.roadmap_version, plan.version), style = MaterialTheme.typography.labelLarge)

        if (state.planChangeNote.isNotBlank()) {
            SectionCard(title = sr(R.string.plan_change_note)) {
                Text(text = state.planChangeNote, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (plan.reason.isNotBlank()) {
            SectionCard(title = sr(R.string.plan_reason)) {
                Text(text = plan.reason, style = MaterialTheme.typography.bodySmall)
            }
        }

        for (week in weeks.keys.sorted()) {
            val items = weeks.getValue(week).sortedWith(compareBy({ it.day ?: 0 }, { it.type }))
            if (items.isEmpty()) continue
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = sr(R.string.week_n, week + 1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    items.forEach { item ->
                        val skillName = item.skillId?.let { content.graph.skills[it]?.name } ?: item.misconceptionId ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = whyLabel, role = Role.Button) { why = item }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TypeBadge(item.type)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = skillName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = sr(R.string.minutes, item.minutes) + "  " + sr(R.string.why_this),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (plan.dropped.isNotEmpty()) {
            SectionCard(title = sr(R.string.roadmap_dropped)) {
                Text(text = sr(R.string.roadmap_dropped_hint), style = MaterialTheme.typography.bodySmall)
                plan.dropped.forEach { item ->
                    val skillName = item.skillId?.let { content.graph.skills[it]?.name } ?: item.skillId ?: ""
                    Text(
                        text = "$skillName: ${item.reason}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Button(onClick = { state.go(com.sahay.app.state.Screen.Today) }, modifier = Modifier.fillMaxWidth()) {
            Text(sr(R.string.roadmap_start_today))
        }
    }

    val selected = why
    if (selected != null) {
        val skill = selected.skillId?.let { content.graph.skills[it] }
        AlertDialog(
            onDismissRequest = { why = null },
            title = { Text(skill?.name ?: selected.skillId ?: "") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = selected.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = sr(R.string.why_this_prereq) + ": " + (
                            skill?.prereqs?.joinToString(", ") { content.graph.skills[it]?.name ?: it }
                                ?: sr(R.string.why_this_none)
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    skill?.notes?.let { notes ->
                        Text(text = sr(R.string.why_this_notes) + ": " + notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { why = null }) { Text(sr(R.string.close)) } },
        )
    }
}

@Composable
private fun TypeBadge(type: String) {
    val label = when (type) {
        "learn" -> sr(R.string.item_learn)
        "practice" -> sr(R.string.item_practice)
        "revise" -> sr(R.string.item_revise)
        "repair" -> sr(R.string.item_repair)
        else -> type
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
