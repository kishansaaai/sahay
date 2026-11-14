package com.sahay.app.llm

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.sahay.engine.GovMode
import com.sahay.engine.Governor
import com.sahay.engine.LlmOrchestrator
import com.sahay.engine.LlmRuntime
import com.sahay.engine.ModelManager
import com.sahay.engine.ModelVerifier
import com.sahay.engine.PromptLibrary
import com.sahay.engine.TokenListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sahay.engine.GenStats
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/** Runtime used when ExecuTorch is not built in (default builds, CI): answers with a fixed notice so the app still runs. */
class UnavailableRuntime : LlmRuntime {
    override val isLoaded = false
    override fun load(modelPath: String, tokenizerPath: String) = throw IllegalStateException("ExecuTorch runtime not built in; build with -Psahay.executorch=true")
    override fun generate(prompt: String, maxNewTokens: Int, temperature: Double, listener: TokenListener) = throw IllegalStateException("no runtime")
    override fun stop() {}
    override fun unload() {}
}

/**
 * Single entry point to the on-device LLM for the rest of the app (issues A04, A05, A08, A13, A19).
 * Call [init] once from the Application or first Activity. Model files live in filesDir/models and are never bundled in the APK.
 */
object SahayLlm {
    const val MODEL_FILE = "llama3_2_1b_int4.pte"
    const val TOKENIZER_FILE = "tokenizer.model"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var lastStats: GenStats? = null; private set
    @Volatile var lastChargeMah: Double? = null; private set
    private var battery: BatteryManager? = null
    private fun charge(): Int? = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.takeIf { it > 0 }


    lateinit var manager: ModelManager; private set
    lateinit var governor: Governor; private set
    lateinit var orchestrator: LlmOrchestrator; private set
    lateinit var monitor: DeviceMonitor; private set

    /** True once [init] finished; screens must not touch the lateinit members before this. */
    @Volatile var ready: Boolean = false
        private set

    /** False when the build has no ExecuTorch runtime, so the UI can say that generated text is unavailable. */
    @Volatile var runtimeAvailable: Boolean = false
        private set

    fun createRuntime(): LlmRuntime = try {
        Class.forName("com.sahay.app.llm.ExecuTorchRuntime").getDeclaredConstructor().newInstance() as LlmRuntime
    } catch (e: ReflectiveOperationException) { UnavailableRuntime() }

    fun init(context: Context, runtime: LlmRuntime = createRuntime()) {
        if (ready) return
        val app = context.applicationContext
        battery = app.getSystemService(BatteryManager::class.java)
        runtimeAvailable = runtime !is UnavailableRuntime
        val dir = File(app.filesDir, "models")
        val model = File(dir, MODEL_FILE)
        val checksums = ModelVerifier.parse(app.assets.open("model_checksums.txt").bufferedReader().use { it.readText() })
        manager = ModelManager(runtime, model.path, File(dir, TOKENIZER_FILE).path, verify = { ModelVerifier.check(model, checksums) })
        governor = Governor(baseThreads = maxOf(1, Runtime.getRuntime().availableProcessors() / 2))
        val lib = PromptLibrary { app.assets.open("prompts/$it").bufferedReader().use { r -> r.readText() } }
        orchestrator = LlmOrchestrator(lib, { prompt, temperature, maxTokens ->
            if (governor.limits().cachedOnly) throw IllegalStateException("governor: paused")
            val sb = StringBuilder()
            generate(prompt, maxTokens, temperature) { sb.append(it); true }
            sb.toString()
        })
        monitor = DeviceMonitor(app, governor)
        monitor.onMode = { mode, _ -> if (mode == GovMode.PAUSE) manager.stop() }
        monitor.start()
        app.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) { scope.launch { manager.onTrimMemory(level) } }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() { scope.launch { manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE) } }
        })
        ready = true
        if (runtimeAvailable && model.isFile) scope.launch {
            runCatching { manager.warmUp() }.onFailure { Log.w("SahayLlm", "warm-up unavailable: ${it.message}") }
        }
    }

    /** Every production path uses the same governor cap and captures measured generation statistics. */
    private fun generate(prompt: String, maxNewTokens: Int, temperature: Double, request: Any? = null, cancelled: () -> Boolean = { false }, listener: TokenListener): GenStats {
        val limits = governor.limits()
        check(!limits.cachedOnly) { "governor: paused" }
        val before = charge()
        val stats = manager.generate(prompt, minOf(maxNewTokens, limits.maxNewTokens), temperature,
            request = request, isCancelled = { cancelled() || governor.limits().cachedOnly }, listener = listener)
        lastStats = stats
        val after = charge()
        lastChargeMah = if (before != null && after != null && after <= before && monitor.last?.charging == false)
            (before - after) / 1000.0 else null
        return stats
    }

    /** A blocking send applies back pressure instead of silently dropping tokens when the UI is busy. */
    fun stream(prompt: String, maxNewTokens: Int, temperature: Double): Flow<String> = callbackFlow {
        val request = Any()
        val worker = Thread {
            try {
                generate(prompt, maxNewTokens, temperature, request = request, cancelled = { isClosedForSend }) { trySendBlocking(it).isSuccess }
                close()
            } catch (e: Throwable) { close(e) }
        }
        worker.start()
        awaitClose { manager.stop(request) }
    }

    val mode: GovMode get() = governor.mode
}
