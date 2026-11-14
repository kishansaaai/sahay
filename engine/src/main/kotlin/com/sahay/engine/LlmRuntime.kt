package com.sahay.engine

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** Receives tokens as they are generated. Return false from [onToken] to ask the runtime to stop. */
fun interface TokenListener { fun onToken(token: String): Boolean }

data class GenStats(val tokens: Int, val ttftMs: Double, val decodeMs: Double, val stopped: Boolean)

/**
 * Contract of the on-device runtime (issue A04). The ExecuTorch JNI wrapper in the app module implements it;
 * tests use a fake. [generate] blocks the calling thread (call it from a background dispatcher) and must return within
 * 500 ms of [stop] being called from another thread.
 */
interface LlmRuntime {
    fun load(modelPath: String, tokenizerPath: String)
    fun generate(prompt: String, maxNewTokens: Int, temperature: Double, listener: TokenListener): GenStats
    fun stop()
    fun unload()
    val isLoaded: Boolean
}

object ModelVerifier {
    fun sha256(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(1 shl 20)
        while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Parse releases/CHECKSUMS.txt (bundled as an asset): file name to expected (sha256, bytes). */
    fun parse(text: String): Map<String, Pair<String, Long>> = text.lineSequence().map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .associate { l -> val p = l.split(Regex("\\s+")); p[1] to (p[0] to p[2].toLong()) }

    /** Null when the file matches its recorded checksum, otherwise the reason it must not be loaded. */
    fun check(file: File, table: Map<String, Pair<String, Long>>): String? {
        val want = table[file.name] ?: return "${file.name} is not in the checksum list"
        if (file.length() != want.second) return "${file.name} has the wrong size (${file.length()} bytes)"
        val got = file.inputStream().use { sha256(it) }
        return if (got == want.first) null else "${file.name} checksum mismatch"
    }
}

/**
 * Model manager (issue A05): loads lazily on first use, warms up with a tiny generation, unloads on memory pressure and
 * reloads on the next request. One generation at a time; every public call is safe from any thread.
 */
class ModelManager(
    private val runtime: LlmRuntime,
    private val modelPath: String,
    private val tokenizerPath: String,
    private val verify: () -> String? = { null },
    private val warmupPrompt: String = "Hello",
) {
    private val lock = Any()
    @Volatile var warmedUp = false
        private set
    @Volatile var loadCount = 0
        private set
    @Volatile private var generating = false
    @Volatile private var activeRequest: Any? = null

    val isLoaded: Boolean get() = runtime.isLoaded

    private fun ensureLoaded() {
        if (runtime.isLoaded) return
        verify()?.let { throw IllegalStateException("model failed verification: $it") }
        runtime.load(modelPath, tokenizerPath)
        loadCount++
        warmedUp = false
    }

    /** Run a tiny generation so the first real request is fast. Call at app start from a background thread. */
    fun warmUp() = synchronized(lock) {
        ensureLoaded()
        if (!warmedUp) {
            generating = true
            try { runtime.generate(warmupPrompt, 4, 0.0) { true }; warmedUp = true }
            finally { generating = false }
        }
    }

    fun generate(prompt: String, maxNewTokens: Int, temperature: Double, request: Any? = null, isCancelled: () -> Boolean = { false }, listener: TokenListener): GenStats = synchronized(lock) {
        require(maxNewTokens > 0) { "generation limit must be positive" }
        if (isCancelled()) throw CancellationException("generation cancelled before load")
        ensureLoaded()
        if (isCancelled()) throw CancellationException("generation cancelled before start")
        activeRequest = request
        generating = true
        try { runtime.generate(prompt, maxNewTokens, temperature) { !isCancelled() && listener.onToken(it) } } finally { generating = false; activeRequest = null }
    }

    /** Safe to call from any thread while a generation runs; does not wait for the lock. */
    fun stop(request: Any? = null) { if (generating && (request == null || activeRequest === request)) runtime.stop() }

    /** Forward Android onTrimMemory. At level 10 (RUNNING_LOW) or worse, stop any generation and free the model. */
    fun onTrimMemory(level: Int) {
        if (level < TRIM_RUNNING_LOW) return
        stop()
        synchronized(lock) { if (runtime.isLoaded) { runtime.unload(); warmedUp = false } }
    }

    fun release() { stop(); synchronized(lock) { if (runtime.isLoaded) runtime.unload(); warmedUp = false } }

    companion object { const val TRIM_RUNNING_LOW = 10 }
}
