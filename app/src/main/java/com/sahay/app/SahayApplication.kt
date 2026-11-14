package com.sahay.app

import android.app.Application
import android.util.Log
import com.sahay.app.llm.SahayLlm
import com.sahay.app.state.AppState

/**
 * Process-wide owner of the app state (issue B03). Keeping state here means a rotation or a language change does not lose
 * the diagnostic in progress or the tutor conversation.
 */
class SahayApplication : Application() {
    lateinit var state: AppState
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            SahayLlm.init(this)
        } catch (e: Throwable) {
            // The app must still open and explain itself when the runtime or its assets are missing.
            Log.e(TAG, "LLM layer unavailable: ${e.message}", e)
        }
        state = AppState(this)
        state.restore()
    }

    companion object {
        private const val TAG = "SahayApp"
    }
}
