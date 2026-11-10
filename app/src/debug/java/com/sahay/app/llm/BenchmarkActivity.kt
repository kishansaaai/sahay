package com.sahay.app.llm

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import com.sahay.engine.BenchRun
import com.sahay.engine.BenchStats
import java.io.File

/**
 * Benchmark harness v1 (issue A06, PRD 11.1). Debug builds only. Runs 10 times each: a fixed 500-token prompt with 200 generated
 * tokens, reporting median and p95 of time to first token, decode tokens/s, prompt tokens/s and peak RAM, and writes
 * filesDir/bench_<device>.md. Pull and commit it under bench/:
 *   adb shell am start -n com.sahay.app/.llm.BenchmarkActivity --ei runs 10
 *   adb shell run-as com.sahay.app cat files/bench_<device>.md > bench/results/<device>.md
 * Record the conditions by hand in the file (charging, cool or heated, brightness, room temperature).
 */
class BenchmarkActivity : Activity() {
    private lateinit var out: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { textSize = 12f; setPadding(24, 24, 24, 24) }
        setContentView(ScrollView(this).apply { addView(out) })
        val runs = intent.getIntExtra("runs", 10)
        SahayLlm.init(this)
        if (intent.getStringExtra("mode") == "sustained") {
            // Sustained run for A14 (thermal) and A15 (energy): adb shell am start ... --es mode sustained --ei minutes 20 --ez governor true
            val minutes = intent.getIntExtra("minutes", 20)
            val gov = intent.getBooleanExtra("governor", true)
            if (gov) SahayLlm.monitor.start()
            Thread { runSustained(minutes, gov) }.start()
        } else Thread { runBench(runs) }.start()
    }

    private fun log(s: String) { Log.i("SahayBench", s); runOnUiThread { out.append(s + "\n") } }

    private fun ramMb(): Double { val i = Debug.MemoryInfo(); Debug.getMemoryInfo(i); return i.totalPss / 1024.0 }

    private fun runBench(runs: Int) {
        try {
            val prompt500 = ("Explain, in detail and with examples, how a stack data structure works and where it is used. " .repeat(35)).take(2200)
            val results = ArrayList<BenchRun>()
            log("Warming up")
            SahayLlm.manager.warmUp()
            for (i in 1..runs) {
                var peak = ramMb()
                var n = 0
                val st = SahayLlm.manager.generate(prompt500, 200, 0.2) { n++; if (n % 10 == 0) peak = maxOf(peak, ramMb()); true }
                // Prompt processing time is the time to first token; the prompt is about 500 tokens.
                results.add(BenchRun(st.ttftMs, st.tokens, st.decodeMs, 500, st.ttftMs, maxOf(peak, ramMb())))
                log("run $i/$runs: ttft %.0f ms, %.1f tok/s".format(st.ttftMs, results.last().decodeTokPerSec))
            }
            val memClass = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).memoryClass
            val header = "## ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, memory class ${memClass} MB, governor ${SahayLlm.mode}\n\n" +
                "Model: ${SahayLlm.MODEL_FILE}. Conditions: FILL IN (charging, cool or heated, brightness).\n\n"
            val md = header + BenchStats.report(results, "Llama 3.2 1B INT4")
            File(filesDir, "bench_${Build.MODEL.replace(' ', '_')}.md").writeText(md)
            log(md)
        } catch (e: Throwable) { log("Benchmark failed: $e") }
    }

    /** Back-to-back 200-token generations for [minutes], one CSV row per generation: elapsed_s,tok_per_s,thermal,battery_pct,charging,mode,tokens. */
    private fun runSustained(minutes: Int, governorOn: Boolean) {
        try {
            val sb = StringBuilder("elapsed_s,tok_per_s,thermal,battery_pct,charging,mode,tokens\n")
            val prompt = "Explain, step by step and with an example, how a queue and a stack differ. ".repeat(10)
            SahayLlm.manager.warmUp()
            val start = System.nanoTime()
            var totalTokens = 0
            val b0 = SahayLlm.monitor.read().batteryPct
            while ((System.nanoTime() - start) / 1e9 < minutes * 60) {
                val lim = SahayLlm.governor.limits()
                if (governorOn && lim.cachedOnly) { Thread.sleep(5_000); continue } // paused: cooling down
                val cap = if (governorOn) lim.maxNewTokens else 200
                val st = SahayLlm.manager.generate(prompt, cap, 0.2) { true }
                val s = SahayLlm.monitor.read()
                val tps = if (st.decodeMs > 0) st.tokens * 1000.0 / st.decodeMs else 0.0
                totalTokens += st.tokens
                val row = "%.1f,%.2f,%d,%d,%b,%s,%d".format((System.nanoTime() - start) / 1e9, tps, s.thermal, s.batteryPct, s.charging, if (governorOn) SahayLlm.mode.name else "OFF", st.tokens)
                sb.append(row).append('\n'); Log.i("SahayBench", row)
            }
            val b1 = SahayLlm.monitor.read().batteryPct
            File(filesDir, "sustained_${if (governorOn) "governor_on" else "governor_off"}.csv").writeText(sb.toString())
            log("done: $totalTokens tokens, battery $b0% -> $b1%")
        } catch (e: Throwable) { log("Sustained run failed: $e") }
    }
}
