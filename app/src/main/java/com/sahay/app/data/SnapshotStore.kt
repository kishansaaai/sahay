package com.sahay.app.data

import android.content.Context
import android.util.Log
import com.sahay.engine.Snapshot
import java.io.File

/**
 * File-backed learner store (issue B04).
 *
 * The PRD asks for on-device persistence of every entity. Room would add an annotation processor, a schema directory and
 * platform tests that cannot run in this repository's CI (no emulator), so the app stores one dependency-free JSON snapshot
 * instead: profile, attempt log (the single source of truth), teach-backs, sessions and the current plan. Mastery and
 * misconception state are rebuilt by replaying attempts, which keeps export/import lossless and testable in the engine's
 * plain-JVM test suite. See engine/src/main/kotlin/com/sahay/engine/Store.kt.
 */
class SnapshotStore(context: Context) {
    private val app: Context = context.applicationContext
    val file: File = File(app.filesDir, FILE_NAME)

    /** Last load error, so Settings can show why an import or a damaged file was rejected. */
    @Volatile var lastError: String? = null
        private set

    fun exists(): Boolean = file.isFile

    /** Returns null when there is no snapshot yet. A damaged snapshot is reported in [lastError] and ignored. */
    fun load(): Snapshot? {
        if (!file.isFile) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            Snapshot.fromJson(text).also { lastError = null }
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            Log.w(TAG, "snapshot rejected: $lastError")
            null
        }
    }

    /** Write to a temporary file first so a crash mid-write cannot truncate the only copy. */
    fun save(snapshot: Snapshot): Boolean = saveJson(snapshot.toJson())

    fun saveJson(json: String): Boolean = try {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (file.exists() && !file.delete()) throw java.io.IOException("could not replace ${file.name}")
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
        lastError = null
        true
    } catch (e: Exception) {
        lastError = e.message ?: e.javaClass.simpleName
        Log.e(TAG, "could not save snapshot", e)
        false
    }

    /** One-tap delete (PRD section 14): snapshot, downloaded model files and any cached copy. */
    fun deleteAll() {
        runCatching { file.delete() }
        runCatching { File(app.filesDir, "models").deleteRecursively() }
        runCatching { File(app.cacheDir, "sahay").deleteRecursively() }
        lastError = null
    }

    companion object {
        const val FILE_NAME = "sahay_snapshot.json"
        private const val TAG = "SahayStore"
    }
}
