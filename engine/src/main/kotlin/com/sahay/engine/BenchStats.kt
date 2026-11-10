package com.sahay.engine

import kotlin.math.ceil

/** One generation measurement (PRD 11.1). Times in milliseconds. */
data class BenchRun(val ttftMs: Double, val decodeTokens: Int, val decodeMs: Double, val promptTokens: Int, val promptMs: Double, val peakRamMb: Double) {
    val decodeTokPerSec: Double get() = if (decodeMs <= 0) 0.0 else decodeTokens * 1000.0 / decodeMs
    val promptTokPerSec: Double get() = if (promptMs <= 0) 0.0 else promptTokens * 1000.0 / promptMs
}

data class Summary(val median: Double, val p95: Double, val min: Double, val max: Double)

object BenchStats {
    /** Nearest-rank percentile (p in 0..100) of a non-empty list. */
    fun percentile(values: List<Double>, p: Double): Double {
        require(values.isNotEmpty()) { "no values" }
        val s = values.sorted()
        val rank = ceil(p / 100.0 * s.size).toInt().coerceIn(1, s.size)
        return s[rank - 1]
    }

    fun median(values: List<Double>): Double {
        val s = values.sorted(); val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    fun summarize(values: List<Double>) = Summary(median(values), percentile(values, 95.0), values.min(), values.max())

    /** Markdown table for bench/results: median and p95 over the runs. */
    fun report(runs: List<BenchRun>, title: String): String {
        fun row(name: String, unit: String, f: (BenchRun) -> Double): String {
            val s = summarize(runs.map(f)); return "| $name | $unit | %.2f | %.2f | %.2f | %.2f |\n".format(java.util.Locale.ROOT, s.median, s.p95, s.min, s.max)
        }
        return "### $title (${runs.size} runs)\n\n| Metric | Unit | Median | p95 | Min | Max |\n| --- | --- | --- | --- | --- | --- |\n" +
            row("Time to first token", "ms") { it.ttftMs } + row("Decode speed", "tokens/s") { it.decodeTokPerSec } +
            row("Prompt processing", "tokens/s") { it.promptTokPerSec } + row("Peak RAM", "MB") { it.peakRamMb }
    }
}
