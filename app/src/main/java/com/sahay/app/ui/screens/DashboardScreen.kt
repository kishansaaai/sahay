package com.sahay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.MasteryBar
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr
import com.sahay.engine.CALIBRATED
import com.sahay.engine.ForecastBasis
import com.sahay.engine.OVERCONFIDENT
import com.sahay.engine.UNDERCONFIDENT

/**
 * Dashboard (issue B10, C7): per-skill mastery, a forecast that refuses to invent a number, and confidence calibration.
 * This is a plain list of bars rather than a chart library, so it stays accessible and dependency-free.
 */
@Composable
fun DashboardScreen(state: AppState) {
    val profile = state.profile
    if (profile == null) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(text = sr(R.string.dashboard_no_profile), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val average = state.averageMastery()
    val forecast = state.forecast()
    val skills = state.goalSkills()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.dashboard_title), style = MaterialTheme.typography.headlineSmall)

        SectionCard(title = sr(R.string.dashboard_mastery)) {
            Text(
                text = sr(R.string.dashboard_average, AppState.percent(average)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            MasteryBar(
                fraction = average,
                label = sr(R.string.dashboard_average, AppState.percent(average)),
            )
            Text(
                text = sr(R.string.dashboard_target, AppState.percent(profile.targetMastery)) + "  " +
                    sr(R.string.dashboard_gap, AppState.percent((profile.targetMastery - average).coerceAtLeast(0.0))),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionCard(title = sr(R.string.dashboard_forecast)) {
            val f = forecast
            if (f == null) {
                Text(text = sr(R.string.forecast_not_enough), style = MaterialTheme.typography.bodyMedium)
            } else {
                val line = when (f.basis) {
                    ForecastBasis.ESTIMATE -> sr(R.string.forecast_estimate, f.daysToTarget ?: 0)
                    ForecastBasis.TARGET_REACHED -> sr(R.string.forecast_reached)
                    ForecastBasis.NOT_ENOUGH_DAYS -> sr(R.string.forecast_not_enough)
                    ForecastBasis.NO_TREND -> sr(R.string.forecast_no_trend)
                    ForecastBasis.TOO_FAR -> sr(R.string.forecast_too_far)
                }
                Text(text = line, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = sr(R.string.forecast_basis, f.trajectory.size),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionCard(title = sr(R.string.dashboard_calibration)) {
            val cal = state.calibration
            if (cal.n == 0) {
                Text(text = sr(R.string.dashboard_calibration_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = sr(R.string.dashboard_calibration_label, calibrationLabel(state)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = sr(R.string.dashboard_calibration_n, cal.n), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = sr(R.string.dashboard_bias, AppState.percent(cal.bias())),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = sr(R.string.dashboard_brier, "%.2f".format(cal.brier())),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionCard(title = sr(R.string.dashboard_misconceptions)) {
            val active = state.activeMisconceptions()
            if (active.isEmpty()) Text(sr(R.string.dashboard_no_misconceptions))
            else active.forEach { Text(state.misconceptionText(it)) }
        }
        SectionCard(title = sr(R.string.dashboard_skills)) {
            if (skills.isEmpty()) {
                Text(text = sr(R.string.dashboard_no_skills), style = MaterialTheme.typography.bodyMedium)
            }
            skills.forEach { id ->
                val name = state.contentFor(state.domain).graph.skills[id]?.name ?: id
                val value = state.mastery(id)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = sr(R.string.dashboard_percent, AppState.percent(value)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (state.needsRevision(id)) RevisionBadge()
                    }
                    MasteryBar(
                        fraction = value,
                        label = sr(R.string.dashboard_skill_mastery, name, AppState.percent(value)),
                    )
                    Text(
                        text = sr(R.string.dashboard_answers, state.answersFor(id)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun calibrationLabel(state: AppState): String = when (state.calibration.label()) {
    OVERCONFIDENT -> sr(R.string.calibration_overconfident)
    UNDERCONFIDENT -> sr(R.string.calibration_underconfident)
    CALIBRATED -> sr(R.string.calibration_calibrated)
    else -> state.calibration.label()
}

@Composable
private fun RevisionBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = sr(R.string.dashboard_revise),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
