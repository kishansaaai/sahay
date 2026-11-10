package com.sahay.app.llm

import com.sahay.engine.GenStats
import com.sahay.engine.LlmRuntime
import com.sahay.engine.TokenListener
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule

/**
 * ExecuTorch implementation of [LlmRuntime] (issue A04). Compiled only with `-Psahay.executorch=true`, see app/build.gradle.kts.
 *
 * Written against the LlmModule API of the official Android Llama demo: LlmModule(modelPath, tokenizerPath, temperature),
 * load(), generate(prompt, seqLen, callback), stop(), resetNative(). The class and constructor names move between ExecuTorch
 * releases: when A01 pins the tag, check them against that tag's examples/demo-apps/android/LlamaDemo. In that API the sampling
 * temperature is fixed when the module is created, so [load] uses [temperature]; newer tags accept it per call (then pass it in generate).
 */
class ExecuTorchRuntime(private val temperature: Float = 0.2f) : LlmRuntime {
    private var module: LlmModule? = null

    @Volatile private var stopRequested = false

    override val isLoaded: Boolean get() = module != null

    @Synchronized
    override fun load(modelPath: String, tokenizerPath: String) {
        if (module != null) return
        val m = LlmModule(modelPath, tokenizerPath, temperature)
        val rc = m.load()
        check(rc == 0) { "ExecuTorch load failed with code $rc" }
        module = m
    }

    override fun generate(prompt: String, maxNewTokens: Int, temperature: Double, listener: TokenListener): GenStats {
        val m = module ?: error("model not loaded")
        stopRequested = false
        val t0 = System.nanoTime()
        var first = 0L
        var n = 0
        m.generate(prompt, maxNewTokens, object : LlmCallback {
            override fun onResult(result: String) {
                if (first == 0L) first = System.nanoTime()
                n++
                if (!listener.onToken(result)) { stopRequested = true; m.stop() }
            }
            override fun onStats(stats: String) {}
        })
        val end = System.nanoTime()
        val ttft = if (first == 0L) 0.0 else (first - t0) / 1e6
        return GenStats(n, ttft, if (first == 0L) 0.0 else (end - first) / 1e6, stopRequested)
    }

    /** Safe from any thread. The runner checks the stop flag between tokens, so it returns within one token time (verify on device: under 500 ms). */
    override fun stop() { stopRequested = true; module?.stop() }

    @Synchronized
    override fun unload() { module?.resetNative(); module = null }
}
