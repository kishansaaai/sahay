package com.sahay.app.state

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Exercise the live app state, ordered writer and replay together, without substituting fake learning decisions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppStateTest {
    private fun await(message: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(message, condition())
    }

    @Test fun diagnosticPracticeImportRestartAndDeletePreserveLearningFacts() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("sahay.db")
        val state = AppState(app)
        state.restore()
        await("startup did not complete") { !state.restoring }
        state.createProfile("DSA", "dsa", "beginner", 6.0, 8, listOf("cricket"))
        while (state.screen == Screen.Diagnostic) {
            val question = state.question!!
            state.chooseOption(question.answer)
            state.updateConfidence(80.0)
            state.submitDiagnosticAnswer()
            val count = state.attempts.size
            state.submitDiagnosticAnswer()
            assertEquals("duplicate submission was recorded", count, state.attempts.size)
            state.nextDiagnosticQuestion()
        }
        assertNotNull(state.plan)
        val rated = state.calibration.n
        state.openStudy("dsa.arrays", practice = true)
        state.chooseCheckOption(state.studyQuestion!!.answer)
        state.submitCheckAnswer()
        assertEquals(rated, state.calibration.n)
        val attemptCount = state.attempts.size
        val json = state.exportJson()
        var imported = false
        state.importSnapshot(json) { error -> assertNull(error); imported = true }
        await("ordered import did not complete") { imported && !state.restoring }
        assertEquals(attemptCount, state.attempts.size)
        assertEquals("practice invented a confidence rating after replay", rated, state.calibration.n)
        val restarted = AppState(app)
        restarted.restore()
        await("restart did not complete") { !restarted.restoring }
        assertEquals(json, restarted.exportJson())
        restarted.deleteAllData()
        await("deletion did not complete") { !restarted.restoring }
        assertNull(restarted.profile)
        val fresh = AppState(app)
        fresh.restore()
        await("fresh startup did not complete") { !fresh.restoring }
        assertNull("queued writes resurrected a deleted learner", fresh.profile)
    }
}
