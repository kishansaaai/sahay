package com.sahay.app.llm

import com.sahay.engine.GenStats
import com.sahay.engine.LlmRuntime
import com.sahay.engine.TokenListener
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule

/**
 * ExecuTorch implementation of [LlmRuntime] (issue A04). Compiled only with `-Psahay.executorch=true`, see app/build.gradle.kts.
 *
 * Written against the LlmModule API of the official Android Llama demo: LlmModule(modelPath, tokenizerPath, samplingTemperature),
 * load(), generate(prompt, seqLen, callback), stop(), resetNative(). The class and constructor names move between ExecuTorch
 * releases: when A01 pins the tag, check them against that tag's examples/demo-apps/android/LlamaDemo. In that API the sampling
 * temperature is fixed when the module is created, so [load] uses [temperature]; newer tags accept it per call (then pass it in generate).
 */
class ExecuTorchRuntime(private val temperature: Float = 0.2f) : LlmRuntime {
    @Volatile private var module: LlmModule? = null
    private var modelPath = ""
    private var tokenizerPath = ""
    private var samplingTemperature = temperature

    @Volatile private var stopRequested = false

    override val isLoaded: Boolean get() = module != null

    @Synchronized
    override fun load(modelPath: String, tokenizerPath: String) {
        if (module != null) return
        val m = LlmModule(modelPath, tokenizerPath, samplingTemperature)
        val rc = m.load()
        if (rc != 0) { m.resetNative(); error("ExecuTorch load failed with code $rc") }
        module = m
        this.modelPath = modelPath
        this.tokenizerPath = tokenizerPath
    }

    override fun generate(prompt: String, maxNewTokens: Int, temperature: Double, listener: TokenListener): GenStats {
        require(temperature.isFinite() && temperature >= 0) { "invalid temperature" }
        if (samplingTemperature != temperature.toFloat()) {
            unload()
            samplingTemperature = temperature.toFloat()
            val m = LlmModule(modelPath, tokenizerPath, samplingTemperature)
            val rc = m.load()
            if (rc != 0) { m.resetNative(); error("ExecuTorch load failed with code $rc") }
            module = m
        }
        val m = module ?: error("model not loaded")
        stopRequested = false
        val t0 = System.nanoTime()
        var first = 0L
        var n = 0
        // v0.7 seqLen is TOTAL context, not a new-token count. Stop at the requested output cap.
        val rc = m.generate(prompt, 2048, object : LlmCallback {
            override fun onResult(result: String) {
                if (result.isEmpty()) return
                if (first == 0L) first = System.nanoTime()
                n++
                if (!listener.onToken(result) || n >= maxNewTokens) { stopRequested = true; m.stop() }
            }
            override fun onStats(stats: String) {}
        }, false) // Never echo the prompt into the learner-facing answer.
        check(rc == 0) { "ExecuTorch generation failed with code $rc" }
        val end = System.nanoTime()
        val ttft = if (first == 0L) 0.0 else (first - t0) / 1e6
        return GenStats(n, ttft, if (first == 0L) 0.0 else (end - first) / 1e6, stopRequested)
    }

    /** Safe from any thread. The runner checks the stop flag between tokens, so it returns within one token time (verify on device: under 500 ms). */
    override fun stop() { stopRequested = true; module?.stop() }

    @Synchronized
    override fun unload() { module?.resetNative(); module = null }
}
