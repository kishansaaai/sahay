package com.sahay.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

// Mirrors res/values/colors.xml so the XML theme and Compose agree (B03).
private val LightColors = lightColorScheme(
    primary = Color(0xFF0F62FE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF1C1B1F),
    secondary = Color(0xFF6750A4),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC0FF),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF0B3B7A),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFCFBCFF),
    onSecondary = Color(0xFF2C1B5B),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E6),
)

internal fun sahayColorScheme(dark: Boolean) = if (dark) DarkColors else LightColors

@Composable
fun SahayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = sahayColorScheme(darkTheme)
    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
