@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sahay.app.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.MasteryBar
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr

/** Today screen (issue B09): one clear next action with its estimated minutes, plus the day's context. */
@Composable
fun TodayScreen(state: AppState) {
    val profile = state.profile
    if (profile == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = sr(R.string.today_title), style = MaterialTheme.typography.headlineSmall)
            Text(text = sr(R.string.today_no_plan), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { state.startIntake() }) { Text(sr(R.string.welcome_start)) }
        }
        return
    }

    val item = state.nextPlanItem()
    val content = state.contentFor(state.domain)
    val skillName = item?.skillId?.let { content.graph.skills[it]?.name } ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.today_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = profile.goal, style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sr(R.string.today_mastery, AppState.percent(state.averageMastery())),
                    fontWeight = FontWeight.SemiBold,
                    // The bar below carries the same sentence as its content description; hide this copy from TalkBack.
                    modifier = Modifier.clearAndSetSemantics {},
                )
                MasteryBar(
                    fraction = state.averageMastery(),
                    label = sr(R.string.today_mastery, AppState.percent(state.averageMastery())),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = sr(R.string.today_streak, state.streakDays()), style = MaterialTheme.typography.bodySmall)
                    Text(text = sr(R.string.today_attempts, state.attempts.size), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.plan == null) {
            SectionCard(title = sr(R.string.today_next)) {
                Text(text = sr(R.string.today_no_plan), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { state.startDiagnostic() }) { Text(sr(R.string.intake_finish)) }
            }
        } else if (item == null) {
            SectionCard(title = sr(R.string.today_next)) {
                Text(text = sr(R.string.today_all_done), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            SectionCard(title = sr(R.string.today_next)) {
                Text(text = skillName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = sr(R.string.minutes, item.minutes) + "  " + sr(R.string.mastery_label, AppState.percent(state.mastery(item.skillId ?: ""))),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { state.openStudy(item.skillId, practice = false) }) { Text(sr(R.string.today_start)) }
                    OutlinedButton(onClick = { state.openStudy(item.skillId, practice = true) }) { Text(sr(R.string.today_practice)) }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Roadmap) }) { Text(sr(R.string.nav_roadmap)) }
            OutlinedButton(onClick = { state.openTutor(item?.skillId) }) { Text(sr(R.string.today_tutor)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Recover) }) { Text(sr(R.string.today_recover)) }
            OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Dashboard) }) { Text(sr(R.string.nav_dashboard)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Settings) }) { Text(sr(R.string.today_settings)) }
            OutlinedButton(onClick = { state.go(com.sahay.app.state.Screen.Device) }) { Text(sr(R.string.today_device)) }
        }
        OutlinedButton(
            onClick = { state.go(com.sahay.app.state.Screen.Share) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(sr(R.string.today_share)) }
    }
}
