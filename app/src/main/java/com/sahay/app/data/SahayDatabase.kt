package com.sahay.app.data

import androidx.room.*
import com.sahay.engine.*

// Canonical content and the derived caches are separate from the authoritative attempt log.
@Entity(tableName = "skills") data class SkillEntity(@PrimaryKey val id: String, val domain: String, val name: String, val notes: String)
@Entity(tableName = "questions") data class QuestionEntity(@PrimaryKey val id: String, val skillId: String, val stem: String, val answer: Int)
@Entity(tableName = "learner_profile") data class ProfileEntity(@PrimaryKey val id: Int = 1, val metadataJson: String)
@Entity(tableName = "skill_states") data class SkillStateEntity(@PrimaryKey val skillId: String, val mastery: Double, val stability: Double, val lastSeen: Double?, val attempts: Int)
@Entity(tableName = "misconception_states") data class MisconceptionStateEntity(@PrimaryKey val id: String, val evidence: Double, val confidentHits: Int, val resolved: Boolean)
@Entity(tableName = "attempts") data class AttemptEntity(@PrimaryKey val seq: Int, val questionId: String, val skillId: String, val payload: String)
@Entity(tableName = "plans") data class PlanEntity(@PrimaryKey val version: Int, val metadataJson: String)
@Entity(tableName = "plan_items") data class PlanItemEntity(@PrimaryKey val position: Int, val version: Int, val dropped: Boolean, val payload: String)
@Entity(tableName = "sessions") data class SessionEntity(@PrimaryKey val id: Int, val payload: String)
@Entity(tableName = "teach_backs") data class TeachBackEntity(@PrimaryKey val seq: Int, val skillId: String, val payload: String)

@Dao
abstract class LearnerDao {
    @Query("SELECT * FROM learner_profile WHERE id = 1") abstract fun metadata(): ProfileEntity?
    @Query("SELECT * FROM attempts ORDER BY seq") abstract fun attempts(): List<AttemptEntity>
    @Query("SELECT * FROM plans ORDER BY version DESC LIMIT 1") abstract fun plan(): PlanEntity?
    @Query("SELECT * FROM plan_items ORDER BY position") abstract fun items(): List<PlanItemEntity>
    @Query("SELECT * FROM teach_backs ORDER BY seq") abstract fun teachBacks(): List<TeachBackEntity>
    @Query("SELECT * FROM sessions ORDER BY id") abstract fun sessions(): List<SessionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun putProfile(row: ProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun putSkills(rows: List<SkillEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun putQuestions(rows: List<QuestionEntity>)
    @Insert abstract fun putAttempts(rows: List<AttemptEntity>)
    @Insert abstract fun putSkillStates(rows: List<SkillStateEntity>)
    @Insert abstract fun putMisconceptions(rows: List<MisconceptionStateEntity>)
    @Insert abstract fun putPlan(row: PlanEntity)
    @Insert abstract fun putItems(rows: List<PlanItemEntity>)
    @Insert abstract fun putTeachBacks(rows: List<TeachBackEntity>)
    @Insert abstract fun putSessions(rows: List<SessionEntity>)
    @Query("DELETE FROM attempts") abstract fun clearAttempts()
    @Query("DELETE FROM skill_states") abstract fun clearSkillStates()
    @Query("DELETE FROM misconception_states") abstract fun clearMisconceptions()
    @Query("DELETE FROM plans") abstract fun clearPlans()
    @Query("DELETE FROM plan_items") abstract fun clearItems()
    @Query("DELETE FROM teach_backs") abstract fun clearTeachBacks()
    @Query("DELETE FROM sessions") abstract fun clearSessions()

    @Transaction
    open fun read(): Snapshot? {
        val meta = metadata() ?: return null
        val root = objectMap(meta.metadataJson).toMutableMap()
        root["attempts"] = attempts().map { Json.parse(it.payload) }
        root["teach_backs"] = teachBacks().map { Json.parse(it.payload) }
        root["sessions"] = sessions().map { Json.parse(it.payload) }
        root["plan"] = plan()?.let { row ->
            objectMap(row.metadataJson).toMutableMap().apply {
                val all = items().filter { it.version == row.version }
                put("items", all.filter { !it.dropped }.map { Json.parse(it.payload) })
                put("dropped", all.filter { it.dropped }.map { Json.parse(it.payload) })
            }
        }
        return Snapshot.fromJson(JsonOut.write(root))
    }

    @Transaction
    open fun replace(snapshot: Snapshot, content: Content?) {
        if (content != null) require(snapshot.validationErrors(content).isEmpty()) { snapshot.validationErrors(content).first() }
        val root = objectMap(snapshot.toJson()).toMutableMap()
        fun rows(key: String) = (root.remove(key) as List<*>).map { it as Map<String, Any?> }
        val attempts = rows("attempts")
        val teachBacks = rows("teach_backs")
        val sessions = rows("sessions")
        val plan = root.remove("plan") as? Map<String, Any?>
        clearAttempts(); clearTeachBacks(); clearSessions(); clearItems(); clearPlans(); clearSkillStates(); clearMisconceptions()
        putProfile(ProfileEntity(metadataJson = JsonOut.write(root)))
        putAttempts(attempts.map { AttemptEntity((it["seq"] as Number).toInt(), it["question_id"] as String, it["skill_id"] as String, JsonOut.write(it)) })
        putTeachBacks(teachBacks.map { TeachBackEntity((it["seq"] as Number).toInt(), it["skill_id"] as String, JsonOut.write(it)) })
        putSessions(sessions.map { SessionEntity((it["id"] as Number).toInt(), JsonOut.write(it)) })
        if (plan != null) {
            val version = (plan["version"] as Number).toInt()
            putPlan(PlanEntity(version, JsonOut.write(plan - setOf("items", "dropped"))))
            var position = 0
            for (key in listOf("items", "dropped")) putItems((plan[key] as List<*>).map {
                PlanItemEntity(position++, version, key == "dropped", JsonOut.write(it))
            })
        }
        if (content != null) {
            putSkills(content.graph.skills.values.map { SkillEntity(it.id, content.domain, it.name, it.notes) })
            putQuestions(content.questions.values.map { QuestionEntity(it.id, it.skill, it.stem, it.answer) })
            val replay = replay(content, snapshot.targets, snapshot.attempts)
            putSkillStates(replay.model.skills.map { (id, s) -> SkillStateEntity(id, s.p, s.S, s.lastSeen, s.attempts) })
            putMisconceptions(replay.misconceptions.state.map { (id, s) -> MisconceptionStateEntity(id, s.evidence, s.confidentHits, s.resolved) })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun objectMap(json: String) = Json.parse(json) as Map<String, Any?>
}

@Database(entities = [SkillEntity::class, QuestionEntity::class, ProfileEntity::class, SkillStateEntity::class,
    MisconceptionStateEntity::class, AttemptEntity::class, PlanEntity::class, PlanItemEntity::class,
    SessionEntity::class, TeachBackEntity::class], version = 1, exportSchema = true)
abstract class SahayDatabase : RoomDatabase() {
    abstract fun learner(): LearnerDao
}
