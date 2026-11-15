package com.sahay.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.sahay.app.R
import com.sahay.app.SahayApp
import com.sahay.app.state.AppState
import com.sahay.app.state.Screen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Headless Android renders and accessibility semantics, separate from the physical TalkBack acceptance pass. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeTextUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun state(): AppState {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("sahay.db")
        val state = AppState(app)
        state.restore()
        val deadline = System.nanoTime() + 10_000_000_000L
        while (state.restoring && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        check(!state.restoring) { "state failed to load" }
        state.createProfile("DSA", "dsa", "beginner", 6.0, 8, emptyList())
        return state
    }

    private fun render(state: AppState) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                SahayApp(stateOverride = state)
            }
        }
    }

    private fun preview(name: String) {
        val output = File("build/outputs/ui-previews/$name.png")
        checkNotNull(output.parentFile).mkdirs()
        // PixelCopy waits for a hardware window surface, which headless Robolectric does not provide.
        // Draw the actual decor view through Android's software Canvas instead.
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test
    @Config(qualifiers = "w720dp-h360dp-land")
    fun settingsActionsRemainReachableAtDoubleTextSizeInEveryLanguage() {
        val state = state()
        state.chooseOption(1)
        val question = state.question
        state.go(Screen.Settings)
        render(state)
        for (code in listOf("en", "hi", "ta", "te", "bn", "mr")) {
            compose.runOnIdle { state.selectLanguage(code) }
            val resources = state.resources()
            compose.onNodeWithText(resources.getString(R.string.settings_copy)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(resources.getString(R.string.share_title)).performScrollTo().assertIsDisplayed()
            compose.runOnIdle {
                assertSame(question, state.question)
                assertEquals(1, state.pendingChoice)
            }
            preview("settings-$code-landscape-200pct")
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h720dp-port")
    fun diagnosticAnnouncesSelectionAndConfidenceAtDoubleTextSize() {
        val state = state()
        state.selectLanguage("ta")
        render(state)
        val option = state.question!!.options.first().text
        compose.onNodeWithText(option).performScrollTo().performClick().assertIsSelected()
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.performScrollTo().assertIsDisplayed().performSemanticsAction(SemanticsActions.SetProgress) { it(73f) }
        compose.runOnIdle {
            assertEquals(0, state.pendingChoice)
            assertEquals(73.0, state.confidence, 0.0)
        }
        compose.onNodeWithText(state.resources().getString(R.string.diag_submit)).performScrollTo().assertIsDisplayed()
        preview("diagnostic-ta-portrait-200pct")
    }
    @Test
    @Config(qualifiers = "w720dp-h360dp-land-night")
    fun bottomTabsRemainOperableWithLargeTextInBothLongAndShortLabels() {
        val state = state()
        state.buildRoadmap()
        state.go(Screen.Today)
        render(state)
        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        for (code in listOf("en", "hi", "ta", "te", "bn", "mr")) {
            compose.runOnIdle { state.selectLanguage(code) }
            val resources = state.resources()
            compose.onNode(tab and hasText(resources.getString(R.string.nav_dashboard))).performClick().assertIsSelected()
            compose.onNode(tab and hasText(resources.getString(R.string.nav_today))).performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(Screen.Today, state.screen) }
            preview("tabs-$code-landscape-dark-200pct")
        }
    }

}
