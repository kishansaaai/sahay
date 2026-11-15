package com.sahay.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** WCAG AA normal-text contrast using the exact light/dark schemes supplied to the app. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double =
        (max(a.luminance(), b.luminance()) + 0.05) / (min(a.luminance(), b.luminance()) + 0.05)

    @Test fun textAndContainerPairsMeetNormalTextContrastInBothThemes() {
        for (dark in listOf(false, true)) {
            val scheme = sahayColorScheme(dark)
            for ((name, colors) in mapOf(
                "primary" to (scheme.onPrimary to scheme.primary),
                "primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "secondary" to (scheme.onSecondary to scheme.secondary),
                "secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "background" to (scheme.onBackground to scheme.background),
                "surface" to (scheme.onSurface to scheme.surface),
                "surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
                "error" to (scheme.onError to scheme.error),
            )) {
                val contrast = ratio(colors.first, colors.second)
                assertTrue("dark=$dark $name contrast=$contrast; requires 4.5", contrast >= 4.5)
            }
        }
    }
}
