package com.sahay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState
import com.sahay.app.ui.LanguageChips
import com.sahay.app.ui.SectionCard
import com.sahay.app.ui.sr

/** Welcome and language screen (PRD 13.1, B19 language entry point). */
@Composable
fun WelcomeScreen(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = sr(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = sr(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = sr(R.string.offline_badge), style = MaterialTheme.typography.titleMedium)
                Text(text = sr(R.string.offline_note), style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard(title = sr(R.string.language)) {
            LanguageChips(state)
            Text(text = sr(R.string.languages_in_build), style = MaterialTheme.typography.bodySmall)
            Text(text = sr(R.string.experimental_language), style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = { state.startIntake() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = sr(R.string.welcome_start))
        }
    }
}
