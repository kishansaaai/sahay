package com.sahay.engine

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Knowledge tracing with forgetting (PRD 9.1 and 9.2, feature C4). Port of engine/sahay_engine/knowledge.py.
 *
 * BKT posterior: correct p' = p(1-s)/(p(1-s)+(1-p)g); wrong p' = ps/(ps+(1-p)(1-g)); learning p_next = p' + (1-p')t.
 * Hints scale the update toward the prior by weights (1.0, 0.7, 0.4, 0). Forgetting: R = exp(-dt/S), p_eff = p*R.
 */
data class Params(
    val p0: Double = 0.2,
    val slip: Double = 0.10,
    val learn: Double = 0.15,
    val s0Days: Double = 2.0,            // ASSUMPTION: initial stability, not given in the PRD
    val reviseBelow: Double = 0.7,
    val hintWeights: List<Double> = listOf(1.0, 0.7, 0.4, 0.0),
    val growthMin: Double = 1.5,
    val growthMax: Double = 2.5,
    val failFactor: Double = 0.5,
    val sMinDays: Double = 1.0,
    val expectedMs: Double = 45000.0,    // ASSUMPTION: reference response time for the speed factor
)

fun bktUpdate(p: Double, correct: Boolean, slip: Double, guess: Double, learn: Double, weight: Double = 1.0): Double {
    val num: Double
    val den: Double
    if (correct) {
        num = p * (1 - slip); den = num + (1 - p) * guess
    } else {
        num = p * slip; den = num + (1 - p) * (1 - guess)
    }
    val post = if (den > 0) num / den else p
    val nxt = post + (1 - post) * learn
    val updated = p + weight * (nxt - p)
    return min(1.0, max(0.0, updated))
}

fun retention(dt: Double, stability: Double): Double = exp(-max(dt, 0.0) / stability)

class SkillState(var p: Double, var S: Double, var lastSeen: Double? = null, var attempts: Int = 0)

class LearnerModel(val params: Params = Params()) {
    val skills = HashMap<String, SkillState>()

    fun state(id: String): SkillState = skills.getOrPut(id) { SkillState(params.p0, params.s0Days) }

    fun pEff(id: String, now: Double): Double {
        val st = state(id)
        val seen = st.lastSeen ?: return st.p
        return st.p * retention(now - seen, st.S)
    }

    /** Update mastery and stability after one answer. [confidence] is 0..100. */
    fun observe(id: String, correct: Boolean, confidence: Double, responseMs: Double, hintLevel: Int, now: Double, nOptions: Int = 4): Double {
        val P = params
        val st = state(id)
        val w = P.hintWeights[min(max(hintLevel, 0), 3)]
        st.p = bktUpdate(st.p, correct, P.slip, 1.0 / nOptions, P.learn, w)
        if (w > 0) {
            if (correct) {
                val conf = min(max(confidence, 0.0), 100.0) / 100.0
                val speed = min(1.0, P.expectedMs / max(responseMs, 1.0))
                val growth = P.growthMin + (P.growthMax - P.growthMin) * (0.5 * conf + 0.5 * speed)
                st.S *= 1.0 + (growth - 1.0) * w     // hints dilute the stability gain
            } else {
                st.S = max(P.sMinDays, P.failFactor * st.S)
            }
        }
        st.lastSeen = now
        st.attempts += 1
        return st.p
    }

    fun needsRevision(id: String, now: Double): Boolean {
        val st = state(id)
        return st.lastSeen != null && pEff(id, now) < params.reviseBelow
    }
}
