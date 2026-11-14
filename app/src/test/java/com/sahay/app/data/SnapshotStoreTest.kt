package com.sahay.app.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sahay.engine.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SnapshotStoreTest {
    private lateinit var app: Application
    private val worker = Executors.newSingleThreadExecutor()
    private fun <T> io(action: () -> T): T = worker.submit<T> { action() }.get()
    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.deleteDatabase("sahay.db")
        File(app.filesDir, SnapshotStore.FILE_NAME).delete()
    }
    @After fun close() { worker.shutdownNow() }
    private fun snapshot(): Snapshot {
        val c = Content.fromJson(app.assets.open("skillgraphs/dsa.json").bufferedReader().use { it.readText() },
            app.assets.open("questions/dsa.json").bufferedReader().use { it.readText() })
        val q = c.bySkill.getValue("dsa.arrays").first()
        return Snapshot(profile = LearnerProfileRow("goal", "dsa", "beginner", 6.0, 5, 70, 0.8, emptyList(), "hi", 1.0),
            targets = listOf(q.skill), attempts = listOf(AttemptRow(1, q.id, q.skill, q.answer, true, 80.0, 40000.0, 0, 0.0)),
            plan = Plan(1, 0.0, "reason", mutableListOf(PlanItem("learn", q.skill, day = 1, minutes = 20))), language = "hi")
    }
    @Test fun transactionRoundTripAndRejectedReplacementPreserveData() = io {
        val store = SnapshotStore(app)
        val snap = snapshot()
        assertTrue(store.saveJson(snap.toJson()))
        assertEquals(snap.toJson(), SnapshotStore(app).load()!!.toJson())
        assertFalse(store.saveJson(snap.copy(attempts = snap.attempts.map { it.copy(correct = false) }).toJson()))
        assertEquals(snap.toJson(), store.load()!!.toJson())
    }
    @Test fun migratesLegacySnapshotOnlyAfterCommit() = io {
        val snap = snapshot()
        val legacy = File(app.filesDir, SnapshotStore.FILE_NAME).apply { writeText(snap.toJson()) }
        assertEquals(snap.toJson(), SnapshotStore(app).load()!!.toJson())
        assertFalse(legacy.exists())
        assertEquals(snap.toJson(), SnapshotStore(app).load()!!.toJson())
    }
    @Test fun deletionRemovesLearnerModelAndTemporaryFiles() = io {
        val store = SnapshotStore(app)
        assertTrue(store.saveJson(snapshot().toJson()))
        File(app.filesDir, "models").mkdirs()
        File(app.filesDir, "models/test.pte").writeText("test")
        File(app.filesDir, SnapshotStore.FILE_NAME + ".tmp").writeText("stale")
        assertTrue(store.deleteAll())
        assertNull(store.load())
        assertFalse(File(app.filesDir, "models").exists())
        assertFalse(File(app.filesDir, SnapshotStore.FILE_NAME + ".tmp").exists())
    }
}
