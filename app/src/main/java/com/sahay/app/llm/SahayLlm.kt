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
        val app = context.applicationContext
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
            manager.generate(prompt, minOf(maxTokens, governor.limits().maxNewTokens), temperature) { sb.append(it); true }
            sb.toString()
        })
        monitor = DeviceMonitor(app, governor)
        app.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) = manager.onTrimMemory(level)
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() = manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        })
        ready = true
    }

    /** Streaming generation for the chat screen. Collect on a background dispatcher; cancelling the collector stops generation. */
    fun stream(prompt: String, maxNewTokens: Int, temperature: Double): Flow<String> = callbackFlow {
        val t = Thread {
            try { manager.generate(prompt, maxNewTokens, temperature) { trySend(it).isSuccess } ; close() } catch (e: Throwable) { close(e) }
        }
        t.start()
        awaitClose { manager.stop() }
    }

    val mode: GovMode get() = governor.mode
}
