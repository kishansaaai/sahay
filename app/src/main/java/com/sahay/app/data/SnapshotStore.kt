package com.sahay.app.data

import android.content.Context
import androidx.room.Room
import com.sahay.engine.Content
import com.sahay.engine.Snapshot
import com.sahay.engine.validationErrors
import java.io.File

/** Room-backed store. All calls run through AppState's ordered IO queue, never on the UI thread. */
class SnapshotStore(context: Context) {
    private val app = context.applicationContext
    private val database = Room.databaseBuilder(app, SahayDatabase::class.java, "sahay.db").build()
    private val dao = database.learner()
    private val legacy = File(app.filesDir, FILE_NAME)
    @Volatile var lastError: String? = null
        private set

    private fun content(snapshot: Snapshot): Content? = snapshot.profile?.let { p ->
        require(p.domain in setOf("dsa", "mleng")) { "unknown domain '${p.domain}'" }
        fun read(path: String) = app.assets.open(path).bufferedReader().use { it.readText() }
        Content.fromJson(read("skillgraphs/${p.domain}.json"), read("questions/${p.domain}.json"))
    }

    fun load(): Snapshot? = try {
        lastError = null
        val current = dao.read()
        if (current != null) current else if (legacy.isFile) {
            val snapshot = Snapshot.fromJson(legacy.readText())
            val c = content(snapshot)
            dao.replace(snapshot, c) // Commit before deleting the legacy source; failures leave it intact.
            check(legacy.delete()) { "migrated data, but could not remove the legacy snapshot" }
            snapshot
        } else null
    } catch (e: Exception) { lastError = e.message; null }

    fun saveJson(json: String): Boolean = try {
        val snapshot = Snapshot.fromJson(json)
        val c = content(snapshot)
        if (c != null) require(snapshot.validationErrors(c).isEmpty()) { snapshot.validationErrors(c).first() }
        dao.replace(snapshot, c)
        lastError = null
        true
    } catch (e: Exception) { lastError = e.message ?: e.javaClass.simpleName; false }

    fun deleteAll(): Boolean = try {
        database.clearAllTables()
        check(!legacy.exists() || legacy.delete()) { "could not delete the legacy snapshot" }
        check(File(app.filesDir, "$FILE_NAME.tmp").let { !it.exists() || it.delete() }) { "could not delete temporary snapshot" }
        for (file in listOf(File(app.filesDir, "models"), File(app.cacheDir, "sahay"))) {
            check(!file.exists() || file.deleteRecursively()) { "could not delete ${file.name}" }
        }
        lastError = null
        true
    } catch (e: Exception) { lastError = e.message ?: e.javaClass.simpleName; false }

    companion object { const val FILE_NAME = "sahay_snapshot.json" }
}
