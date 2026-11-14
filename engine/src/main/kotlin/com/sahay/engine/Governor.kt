package com.sahay.engine

/** Governor modes (PRD 11.3, feature I2). Ordered from least to most restrictive. */
enum class GovMode { FULL, BALANCED, SAVER, PAUSE }

/** Android PowerManager thermal status constants, kept here so the policy has no Android dependency. */
object Thermal { const val NONE = 0; const val LIGHT = 1; const val MODERATE = 2; const val SEVERE = 3; const val CRITICAL = 4 }

data class GovSignals(val thermal: Int, val batteryPct: Int, val charging: Boolean, val freeMemMb: Int)

/** What a mode changes: generation limits and thread count. [cachedOnly] means serve cached or template text, no new generation. */
data class GovLimits(val maxNewTokens: Int, val threads: Int, val contextTokens: Int, val deferNonUrgent: Boolean, val cachedOnly: Boolean)

data class GovEvent(val timeMs: Long, val from: GovMode, val to: GovMode, val reason: String)

/**
 * Thermal, battery and memory governor. Call [update] every few seconds with fresh readings.
 * Getting worse is applied at once; getting better is applied one step at a time and only after [minDwellMs] in the current
 * mode, so the mode does not flap around a threshold. Every change is appended to [log] with its reason (debug panel, issue A13).
 */
class Governor(
    private val baseThreads: Int,
    private val fullMaxTokens: Int = 256,
    private val minDwellMs: Long = 20_000,
    private val fullContext: Int = 2048,
) {
    var mode: GovMode = GovMode.FULL
        private set
    private var changedAt = Long.MIN_VALUE / 2
    private val events = ArrayList<GovEvent>()
    val log: List<GovEvent> get() = events

    /** The mode the readings call for, with the reason, before any hysteresis. */
    fun target(s: GovSignals): Pair<GovMode, String> {
        val onBattery = !s.charging
        if (s.thermal >= Thermal.SEVERE) return GovMode.PAUSE to "thermal status severe (${s.thermal})"
        if (onBattery && s.batteryPct < 5) return GovMode.PAUSE to "battery critical (${s.batteryPct}%)"
        if (s.freeMemMb < 150) return GovMode.PAUSE to "free memory ${s.freeMemMb} MB"
        if (s.thermal == Thermal.MODERATE) return GovMode.SAVER to "moderate thermal throttling"
        if (onBattery && s.batteryPct < 20) return GovMode.SAVER to "battery ${s.batteryPct}% (under 20%)"
        if (s.freeMemMb < 300) return GovMode.SAVER to "free memory ${s.freeMemMb} MB"
        if (s.thermal == Thermal.LIGHT) return GovMode.BALANCED to "light thermal throttling"
        if (onBattery && s.batteryPct < 40) return GovMode.BALANCED to "battery ${s.batteryPct}% (20 to 40%)"
        return GovMode.FULL to "cool, and charging or battery at least 40%"
    }

    /** Returns the (possibly unchanged) mode after applying [s] at time [nowMs]. */
    fun update(s: GovSignals, nowMs: Long): GovMode {
        val (want, why) = target(s)
        val next = when {
            want > mode -> want
            want < mode && nowMs - changedAt >= minDwellMs -> GovMode.values()[mode.ordinal - 1]
            else -> mode
        }
        if (next != mode) {
            val note = if (next == want) why else "$why; easing one step after ${(nowMs - changedAt) / 1000}s"
            events.add(GovEvent(nowMs, mode, next, note))
            mode = next
            changedAt = nowMs
        }
        return mode
    }

    fun limits(m: GovMode = mode): GovLimits = when (m) {
        GovMode.FULL -> GovLimits(fullMaxTokens, baseThreads, fullContext, deferNonUrgent = false, cachedOnly = false)
        GovMode.BALANCED -> GovLimits(fullMaxTokens * 5 / 8, maxOf(1, baseThreads - 1), fullContext, deferNonUrgent = false, cachedOnly = false)
        GovMode.SAVER -> GovLimits(fullMaxTokens * 3 / 8, maxOf(1, baseThreads - 1), fullContext / 2, deferNonUrgent = true, cachedOnly = false)
        GovMode.PAUSE -> GovLimits(0, 1, fullContext / 2, deferNonUrgent = true, cachedOnly = true)
    }

    /** Short text for the status chip. */
    fun chipText(): String = when (mode) {
        GovMode.FULL -> "Full speed"
        GovMode.BALANCED -> "Balanced: phone is warm or battery is lower"
        GovMode.SAVER -> "Saver: shorter answers to protect battery and heat"
        GovMode.PAUSE -> "Cooling down: using saved text for now"
    }
}
