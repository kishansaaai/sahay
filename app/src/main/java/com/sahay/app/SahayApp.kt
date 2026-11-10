package com.sahay.app

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sahay.app.ui.theme.SahayTheme

@Composable
fun SahayApp() {
    SahayTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(text = "Sahay") })
            }
        ) {
            Text(
                text = "On-device adaptive learning companion",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
