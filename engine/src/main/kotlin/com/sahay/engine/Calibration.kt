package com.sahay.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/** Hint ladder and confidence calibration (PRD 9.4, feature I1). Port of calibration.py. */
const val OVERCONFIDENT = "overconfident"
const val UNDERCONFIDENT = "underconfident"
const val CALIBRATED = "calibrated"

data class HintLadder(val levels: List<Int> = listOf(0, 1, 2, 3)) {
    /** [current] is the last level shown, or null before any hint. Returns the next level or null when exhausted. */
    fun nextLevel(current: Int?): Int? = levels.firstOrNull { current == null || it > current }
}

data class CalibrationPolicy(val label: String, val askJustification: Boolean, val encourage: Boolean, val ladder: HintLadder)

class CalibrationTracker {
    private val conf = ArrayList<Double>()
    private val hit = ArrayList<Double>()

    val n: Int get() = conf.size

    fun add(confidence0to100: Double, correct: Boolean) {
        conf.add(min(max(confidence0to100, 0.0), 100.0) / 100.0)
        hit.add(if (correct) 1.0 else 0.0)
    }

    fun bias(): Double = if (n == 0) 0.0 else (conf.sum() - hit.sum()) / n

    fun brier(): Double = if (n == 0) 0.0 else conf.indices.sumOf { (conf[it] - hit[it]).pow(2) } / n

    fun label(): String {
        if (n < MIN_ANSWERS) return CALIBRATED
        val b = round(bias() * 1e9) / 1e9
        return if (b > BIAS_LIMIT) OVERCONFIDENT else if (b < -BIAS_LIMIT) UNDERCONFIDENT else CALIBRATED
    }

    fun policy(): CalibrationPolicy {
        val lab = label()
        return CalibrationPolicy(lab, lab == OVERCONFIDENT, lab == UNDERCONFIDENT,
            HintLadder(if (lab == UNDERCONFIDENT) listOf(0, 2, 3) else listOf(0, 1, 2, 3)))
    }

    companion object {
        const val MIN_ANSWERS = 5       // ASSUMPTION: avoid labelling on too little evidence
        const val BIAS_LIMIT = 0.15
    }
}
