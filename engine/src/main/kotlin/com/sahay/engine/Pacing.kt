package com.sahay.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/** Cognitive-load-aware pacing (PRD 9.7, feature X1). On-device signals only; always dismissible. Port of pacing.py. */
const val LIGHTER = "lighter_activity"
const val BREAK = "break"
const val END_SESSION = "end_session_with_recap"
const val NONE = "none"

data class PacingSuggestion(val fatigue: Double, val action: String, val dismissible: Boolean = true)

class FatigueMonitor {
    private val rts = ArrayList<Double>()
    private val results = ArrayList<Boolean>()

    fun add(responseMs: Double, correct: Boolean) { rts.add(responseMs); results.add(correct) }

    fun errorStreak(): Int {
        var n = 0
        for (r in results.asReversed()) { if (r) break; n++ }
        return n
    }

    private fun median(xs: List<Double>): Double {
        val s = xs.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
    }

    /** 0..1: 0.6 x rise of recent response time against the learner's own median + 0.4 x error streak. */
    fun index(): Double {
        if (rts.size < WINDOW + 3) return 0.0
        val base = median(rts.subList(0, rts.size - WINDOW))
        val recent = median(rts.subList(rts.size - WINDOW, rts.size))
        val rise = if (base > 0) min(1.0, max(0.0, recent / base - 1.0)) else 0.0
        val streak = min(1.0, errorStreak() / 4.0)
        return 0.6 * rise + 0.4 * streak
    }

    fun suggestion(): PacingSuggestion {
        val f = index()
        val act = when {
            f < THRESHOLD -> NONE
            f < 0.75 -> LIGHTER
            f < 0.9 -> BREAK
            else -> END_SESSION
        }
        return PacingSuggestion(round(f * 1000.0) / 1000.0, act, true)   // never blocks the learner
    }

    companion object {
        const val WINDOW = 5
        const val THRESHOLD = 0.6        // ASSUMPTION: tuned later with the simulator and pilot
    }
}
