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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.sahay.app.ui.ChipRow
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr
import com.sahay.engine.Plan

/**
 * Missed-day recovery (issue B11, I6). The screen only chooses a budget and shows the engine's result: the planner decides
 * what to keep, and every dropped skill is listed with the engine's reason.
 */
@Composable
fun RecoverScreen(state: AppState) {
    val before: Plan? = remember { state.plan }
    var budget by remember { mutableStateOf(60) }
    val after = state.plan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.recover_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = sr(R.string.recover_body), style = MaterialTheme.typography.bodyLarge)

        SectionCard(title = sr(R.string.recover_budget)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(30, 60, 120, 240).forEach { minutes ->
                    ChipRow(sr(R.string.minutes, minutes), budget == minutes) { budget = minutes }
                }
            }
            Button(onClick = { state.recoverPlan(budget) }, modifier = Modifier.fillMaxWidth()) {
                Text(sr(R.string.recover_apply))
            }
        }

        if (after == null || after === before) {
            Text(text = sr(R.string.recover_no_change), style = MaterialTheme.typography.bodyMedium)
        } else {
            SectionCard(title = sr(R.string.recover_result)) {
                Text(
                    text = sr(
                        R.string.recover_before_after,
                        before?.items?.size ?: 0,
                        after.items.size,
                        after.dropped.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = after.reason, style = MaterialTheme.typography.bodyMedium)
                Text(text = sr(R.string.recover_note), style = MaterialTheme.typography.bodySmall)
            }
            if (after.dropped.isNotEmpty()) {
                SectionCard(title = sr(R.string.recover_dropped)) {
                    after.dropped.forEach { item ->
                        val name = item.skillId?.let { state.contentFor(state.domain).graph.skills[it]?.name }
                            ?: item.skillId ?: item.misconceptionId ?: ""
                        Text(
                            text = "$name — ${item.reason}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
