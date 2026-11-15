package com.sahay.app

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.sahay.app.state.AppState
import com.sahay.app.state.Screen
import com.sahay.app.ui.screens.DashboardScreen
import com.sahay.app.ui.screens.DeviceScreen
import com.sahay.app.ui.screens.DiagnosticScreen
import com.sahay.app.ui.screens.IntakeScreen
import com.sahay.app.ui.screens.RecoverScreen
import com.sahay.app.ui.screens.RoadmapScreen
import com.sahay.app.ui.screens.ShareScreen
import com.sahay.app.ui.screens.ScanScreen
import com.sahay.app.ui.screens.SettingsScreen
import com.sahay.app.ui.screens.StudyScreen
import com.sahay.app.ui.screens.TodayScreen
import com.sahay.app.ui.screens.TutorScreen
import com.sahay.app.ui.screens.WelcomeScreen
import com.sahay.app.ui.sr
import com.sahay.app.ui.theme.SahayTheme
import java.util.Locale

private val TABS = listOf(Screen.Today, Screen.Roadmap, Screen.Dashboard, Screen.Tutor)

/** App shell (issue B03): top bar, three main tabs and the full-screen flows (welcome, intake, diagnostic, study). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SahayApp(stateOverride: AppState? = null) {
    val state = stateOverride ?: (LocalContext.current.applicationContext as SahayApplication).state
    SahayTheme {
        val base = LocalContext.current
        val localized = remember(base, state.language, base.resources.configuration) {
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(state.language))
            base.createConfigurationContext(config)
        }
        CompositionLocalProvider(LocalContext provides localized) {
            val onTab = state.screen in TABS
            BackHandler(enabled = !onTab && state.screen != Screen.Welcome) { goBack(state) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(screenTitle(state.screen), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            if (!onTab && state.screen != Screen.Welcome) {
                                TextButton(onClick = { goBack(state) }) { Text(sr(R.string.back)) }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (onTab) {
                        NavigationBar {
                            TABS.forEach { tab ->
                                val label = tabLabel(tab)
                                NavigationBarItem(
                                    selected = state.screen == tab,
                                    onClick = { state.go(tab) },
                                    icon = { Text(label.take(1), modifier = Modifier.clearAndSetSemantics {}) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    if (state.restoring) {
                        Text(sr(R.string.loading), modifier = Modifier.padding(24.dp))
                    } else when (state.screen) {
                        Screen.Welcome -> WelcomeScreen(state)
                        Screen.Intake -> IntakeScreen(state)
                        Screen.Diagnostic -> DiagnosticScreen(state)
                        Screen.Today -> TodayScreen(state)
                        Screen.Study -> StudyScreen(state)
                        Screen.Roadmap -> RoadmapScreen(state)
                        Screen.Tutor -> TutorScreen(state)
                        Screen.Dashboard -> DashboardScreen(state)
                        Screen.Recover -> RecoverScreen(state)
                        Screen.Device -> DeviceScreen(state)
                        Screen.Settings -> SettingsScreen(state)
                        Screen.Scan -> ScanScreen(state)
                        Screen.Share -> ShareScreen(state)
                    }
                }
            }
        }
    }
}

private fun goBack(state: AppState) {
    when (state.screen) {
        Screen.Intake -> state.go(Screen.Welcome)
        Screen.Diagnostic -> state.go(if (state.profile == null) Screen.Intake else Screen.Today)
        Screen.Study -> state.go(Screen.Today)
        Screen.Recover -> state.go(Screen.Today)
        Screen.Device -> state.go(if (state.profile == null) Screen.Welcome else Screen.Settings)
        Screen.Settings -> state.go(Screen.Today)
        else -> state.go(Screen.Today)
    }
}

@Composable
private fun screenTitle(screen: Screen): String = when (screen) {
    Screen.Welcome -> sr(R.string.app_name)
    Screen.Intake -> sr(R.string.intake_title)
    Screen.Diagnostic -> sr(R.string.diag_title)
    Screen.Today -> sr(R.string.today_title)
    Screen.Study -> sr(R.string.study_title)
    Screen.Roadmap -> sr(R.string.roadmap_title)
    Screen.Tutor -> sr(R.string.tutor_title)
    Screen.Dashboard -> sr(R.string.dashboard_title)
    Screen.Recover -> sr(R.string.recover_title)
    Screen.Device -> sr(R.string.device_title)
    Screen.Settings -> sr(R.string.settings_title)
    Screen.Scan -> sr(R.string.scan_title)
    Screen.Share -> sr(R.string.share_title)
}

@Composable
private fun tabLabel(screen: Screen): String = when (screen) {
    Screen.Roadmap -> sr(R.string.nav_roadmap)
    Screen.Tutor -> sr(R.string.nav_tutor)
    Screen.Dashboard -> sr(R.string.nav_dashboard)
    else -> sr(R.string.nav_today)
}

@Composable
private fun NotInBuildScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = sr(R.string.not_in_build), style = MaterialTheme.typography.bodyLarge)
    }
}
