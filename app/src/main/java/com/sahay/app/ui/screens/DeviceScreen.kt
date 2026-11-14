package com.sahay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr
import com.sahay.engine.GovMode

/**
 * Device and energy panel (issue B13). It shows what the governor measured and the limits it is currently applying; the
 * thresholds are still the untuned defaults, so the screen says that rather than implying a phone result.
 */
@Composable
fun DeviceScreen(state: AppState) {
    val snapshot = state.deviceSnapshot()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = sr(R.string.device_title), style = MaterialTheme.typography.headlineSmall)
        Text(text = sr(R.string.device_note), style = MaterialTheme.typography.bodyMedium)

        SectionCard(title = sr(R.string.device_signals)) {
            if (snapshot == null) {
                Text(text = sr(R.string.device_no_sample), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = sr(R.string.device_mode, modeLabel(snapshot.mode)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = sr(R.string.device_thermal, snapshot.thermal), style = MaterialTheme.typography.bodyMedium)
                Text(text = sr(R.string.device_battery, snapshot.batteryPct), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (snapshot.charging) sr(R.string.device_charging) else sr(R.string.device_not_charging),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(text = sr(R.string.device_free_mem, snapshot.freeMemMb), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (snapshot != null) {
            SectionCard(title = sr(R.string.device_limits)) {
                Text(text = sr(R.string.device_max_tokens, snapshot.maxNewTokens), style = MaterialTheme.typography.bodyMedium)
                Text(text = sr(R.string.device_threads, snapshot.threads), style = MaterialTheme.typography.bodyMedium)
                Text(text = sr(R.string.device_context, snapshot.contextTokens), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = sr(R.string.device_defer, if (snapshot.deferNonUrgent) sr(R.string.yes) else sr(R.string.no)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = sr(R.string.device_cached, if (snapshot.cachedOnly) sr(R.string.yes) else sr(R.string.no)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
        }
    }
}

@Composable
private fun modeLabel(mode: GovMode): String = when (mode) {
    GovMode.FULL -> sr(R.string.gov_full)
    GovMode.BALANCED -> sr(R.string.gov_balanced)
    GovMode.SAVER -> sr(R.string.gov_saver)
    GovMode.PAUSE -> sr(R.string.gov_pause)
}
