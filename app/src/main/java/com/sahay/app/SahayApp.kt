package com.sahay.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahay.app.ui.theme.SahayTheme

/** App shell (issue B03). The screens from B04 to B13 mount inside this Scaffold. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SahayApp() {
    SahayTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text(text = "Sahay") }) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "On-device adaptive learning companion",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Works without internet. Everything stays on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
