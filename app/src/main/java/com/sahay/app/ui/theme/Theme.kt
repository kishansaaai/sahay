package com.sahay.app.ui.theme

import androidx.compose.foundation.isSystemInLightTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.sahay.app.R

@Composable
fun SahayTheme(
    darkTheme: Boolean = isSystemInLightTheme().not(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> dynamicDarkColorScheme()
        else -> dynamicLightColorScheme()
    }
    val viewContext = LocalContext.current
    SideEffect {
        val window = (viewContext as android.app.Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography,
        content = content
    )
}
