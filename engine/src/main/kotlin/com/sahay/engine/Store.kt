package com.sahay.engine

/**
 * Local persistence model for the app (issue B04, PRD section 8).
 *
 * The snapshot is the on-device record: learner profile, every attempt, teach-back rows, session rows and the current plan.
 * Mastery and misconception state are *derived* by replaying [attempts] through [applyAnswer], so an attempt row is the
 * single source of truth and the learner state can always be rebuilt (and exported/imported) exactly.
 *
 * Everything here is plain Kotlin with no Android or third-party dependency, so the same code runs in the app, in the
 * simulator and in the CI engine tests.
 */
const val SNAPSHOT_SCHEMA = "sahay.snapshot/1"

/** One row of PRD section 8's Attempt entity. [seq] is the monotonic attempt order; [day] is days since the profile was created. */
data class AttemptRow(
    val seq: Int,
    val questionId: String,
    val skillId: String,
    val chosenIndex: Int,
    val correct: Boolean,
    val confidence: Double,
    val responseMs: Double,
    val hintLevel: Int,
    val day: Double,
)

data class TeachBackRow(
    val seq: Int,
    val skillId: String,
    val text: String,
    val accuracy: Int,
    val completeness: Int,
    val clarity: Int,
    val example: Int,
    val feedback: String,
    val day: Double,
)

data class SessionRow(
    val id: Int,
    val startDay: Double,
    val endDay: Double,
    val meanTokensPerSec: Double,
    val energyMahEstimate: Double,
    val governorEvents: Int,
)

/** The intake result (PRD section 8, LearnerProfile). */
data class LearnerProfileRow(
    val goal: String,
    val domain: String,
    val level: String,
    val hoursPerWeek: Double,
    val studyDays: Int,
    val deadlineDay: Int,
    val targetMastery: Double,
    val interests: List<String>,
    val language: String,
    val createdAt: Double,
) {
    fun toPlannerProfile(): Profile = Profile(hoursPerWeek, studyDays, deadlineDay, targetMastery)
}

/** A complete exportable learner snapshot. */
data class Snapshot(
    val profile: LearnerProfileRow? = null,
    val targets: List<String> = emptyList(),
    val attempts: List<AttemptRow> = emptyList(),
    val teachBacks: List<TeachBackRow> = emptyList(),
    val sessions: List<SessionRow> = emptyList(),
    val plan: Plan? = null,
    val planChangeNote: String = "",
    val language: String = "en",
) {
    val nextAttemptSeq: Int get() = (attempts.maxOfOrNull { it.seq } ?: 0) + 1

    fun toJson(): String = JsonOut.write(
        mapOf(
            "schema" to SNAPSHOT_SCHEMA,
            "language" to language,
            "targets" to targets,
            "profile" to profile?.let(::profileToMap),
            "attempts" to attempts.map(::attemptToMap),
            "teach_backs" to teachBacks.map(::teachBackToMap),
            "sessions" to sessions.map(::sessionToMap),
            "plan" to plan?.let(::planToMap),
            "plan_change_note" to planChangeNote,
        ),
    )

    companion object {
        /** Parse an exported snapshot. Throws IllegalArgumentException with a readable reason on malformed input. */
        fun fromJson(text: String): Snapshot {
            val root = asMap(Json.parse(text), "snapshot")
            val schema = root["schema"] as? String
                ?: throw IllegalArgumentException("snapshot is missing the schema field")
            if (schema != SNAPSHOT_SCHEMA) throw IllegalArgumentException("unsupported snapshot schema '$schema' (expected $SNAPSHOT_SCHEMA)")
            return Snapshot(
                profile = root["profile"]?.let { profileFromMap(asMap(it, "profile")) },
                targets = asList(root["targets"] ?: emptyList<Any?>(), "targets").map { it.toString() },
                attempts = asList(root["attempts"] ?: emptyList<Any?>(), "attempts").mapIndexed { i, a -> attemptFromMap(asMap(a, "attempts[$i]")) },
                teachBacks = asList(root["teach_backs"] ?: emptyList<Any?>(), "teach_backs").mapIndexed { i, a -> teachBackFromMap(asMap(a, "teach_backs[$i]")) },
                sessions = asList(root["sessions"] ?: emptyList<Any?>(), "sessions").mapIndexed { i, a -> sessionFromMap(asMap(a, "sessions[$i]")) },
                plan = root["plan"]?.let { planFromMap(asMap(it, "plan")) },
                planChangeNote = (root["plan_change_note"] as? String) ?: "",
                language = (root["language"] as? String) ?: "en",
            )
        }
    }
}

/** State rebuilt from the attempt log. */
class ReplayState(val model: LearnerModel, val misconceptions: MisconceptionEngine, val calibration: CalibrationTracker, val fatigue: FatigueMonitor)

/**
 * Rebuild learner state from attempts in sequence order. Attempts whose question is unknown (for example a snapshot from a
 * newer content pack) are skipped rather than aborting the load.
 */
fun replay(content: Content, targets: Collection<String>, attempts: List<AttemptRow>, params: Params = Params()): ReplayState {
    val model = LearnerModel(params)
    val mis = MisconceptionEngine()
    val cal = CalibrationTracker()
    val fatigue = FatigueMonitor()
    val closure = content.graph.closure(targets).sorted()
    for (a in attempts.sortedBy { it.seq }) {
        val q = content.questions[a.questionId] ?: continue
        if (a.chosenIndex !in q.options.indices) continue
        applyAnswer(content, model, mis, closure, q, a.chosenIndex, a.confidence, a.responseMs, a.hintLevel, a.day)
        cal.add(a.confidence, a.correct)
        fatigue.add(a.responseMs, a.correct)
    }
    return ReplayState(model, mis, cal, fatigue)
}

/** True when every attempt references a known question and a valid option (used before accepting an import). */
fun validateAttempts(content: Content, attempts: List<AttemptRow>): List<String> {
    val errors = ArrayList<String>()
    for (a in attempts) {
        val q = content.questions[a.questionId]
        if (q == null) errors.add("attempt ${a.seq}: unknown question '${a.questionId}'")
        else if (a.chosenIndex !in q.options.indices) errors.add("attempt ${a.seq}: option ${a.chosenIndex} is out of range")
    }
    return errors
}

// ---- JSON encode/decode helpers (kept private so the wire format is one place) ----

private fun asMap(v: Any?, what: String): Map<String, Any?> =
    v as? Map<String, Any?> ?: throw IllegalArgumentException("$what must be a JSON object")

private fun asList(v: Any?, what: String): List<Any?> =
    v as? List<Any?> ?: throw IllegalArgumentException("$what must be a JSON array")

private fun num(v: Any?, what: String): Double = (v as? Number)?.toDouble() ?: throw IllegalArgumentException("$what must be a number")
private fun int(v: Any?, what: String): Int = num(v, what).toInt()
private fun str(v: Any?, what: String): String = v as? String ?: throw IllegalArgumentException("$what must be a string")
private fun bool(v: Any?, what: String): Boolean = v as? Boolean ?: throw IllegalArgumentException("$what must be true or false")

private fun profileToMap(p: LearnerProfileRow): Map<String, Any?> = mapOf(
    "goal" to p.goal, "domain" to p.domain, "level" to p.level, "hours_per_week" to p.hoursPerWeek,
    "study_days" to p.studyDays, "deadline_day" to p.deadlineDay, "target_mastery" to p.targetMastery,
    "interests" to p.interests, "language" to p.language, "created_at" to p.createdAt,
)

private fun profileFromMap(m: Map<String, Any?>): LearnerProfileRow = LearnerProfileRow(
    goal = str(m["goal"], "profile.goal"),
    domain = str(m["domain"], "profile.domain"),
    level = (m["level"] as? String) ?: "intermediate",
    hoursPerWeek = num(m["hours_per_week"] ?: 6.0, "profile.hours_per_week"),
    studyDays = int(m["study_days"] ?: 5, "profile.study_days"),
    deadlineDay = int(m["deadline_day"] ?: 70, "profile.deadline_day"),
    targetMastery = num(m["target_mastery"] ?: 0.8, "profile.target_mastery"),
    interests = asList(m["interests"] ?: emptyList<Any?>(), "profile.interests").map { it.toString() },
    language = (m["language"] as? String) ?: "en",
    createdAt = num(m["created_at"] ?: 0.0, "profile.created_at"),
)

private fun attemptToMap(a: AttemptRow): Map<String, Any?> = mapOf(
    "seq" to a.seq, "question_id" to a.questionId, "skill_id" to a.skillId, "chosen_index" to a.chosenIndex,
    "correct" to a.correct, "confidence" to a.confidence, "response_ms" to a.responseMs, "hint_level" to a.hintLevel, "day" to a.day,
)

private fun attemptFromMap(m: Map<String, Any?>): AttemptRow = AttemptRow(
    seq = int(m["seq"], "attempt.seq"),
    questionId = str(m["question_id"], "attempt.question_id"),
    skillId = str(m["skill_id"], "attempt.skill_id"),
    chosenIndex = int(m["chosen_index"], "attempt.chosen_index"),
    correct = bool(m["correct"], "attempt.correct"),
    confidence = num(m["confidence"], "attempt.confidence"),
    responseMs = num(m["response_ms"], "attempt.response_ms"),
    hintLevel = int(m["hint_level"] ?: 0, "attempt.hint_level"),
    day = num(m["day"], "attempt.day"),
)

private fun teachBackToMap(t: TeachBackRow): Map<String, Any?> = mapOf(
    "seq" to t.seq, "skill_id" to t.skillId, "text" to t.text, "accuracy" to t.accuracy,
    "completeness" to t.completeness, "clarity" to t.clarity, "example" to t.example, "feedback" to t.feedback, "day" to t.day,
)

private fun teachBackFromMap(m: Map<String, Any?>): TeachBackRow = TeachBackRow(
    seq = int(m["seq"], "teach_back.seq"),
    skillId = str(m["skill_id"], "teach_back.skill_id"),
    text = str(m["text"], "teach_back.text"),
    accuracy = int(m["accuracy"] ?: 0, "teach_back.accuracy"),
    completeness = int(m["completeness"] ?: 0, "teach_back.completeness"),
    clarity = int(m["clarity"] ?: 0, "teach_back.clarity"),
    example = int(m["example"] ?: 0, "teach_back.example"),
    feedback = (m["feedback"] as? String) ?: "",
    day = num(m["day"] ?: 0.0, "teach_back.day"),
)

private fun sessionToMap(s: SessionRow): Map<String, Any?> = mapOf(
    "id" to s.id, "start_day" to s.startDay, "end_day" to s.endDay,
    "mean_tokens_per_sec" to s.meanTokensPerSec, "energy_mah_estimate" to s.energyMahEstimate, "governor_events" to s.governorEvents,
)

private fun sessionFromMap(m: Map<String, Any?>): SessionRow = SessionRow(
    id = int(m["id"], "session.id"),
    startDay = num(m["start_day"] ?: 0.0, "session.start_day"),
    endDay = num(m["end_day"] ?: 0.0, "session.end_day"),
    meanTokensPerSec = num(m["mean_tokens_per_sec"] ?: 0.0, "session.mean_tokens_per_sec"),
    energyMahEstimate = num(m["energy_mah_estimate"] ?: 0.0, "session.energy_mah_estimate"),
    governorEvents = int(m["governor_events"] ?: 0, "session.governor_events"),
)

private fun planToMap(p: Plan): Map<String, Any?> = mapOf(
    "version" to p.version,
    "created_day" to p.createdDay,
    "reason" to p.reason,
    "items" to p.items.map(::planItemToMap),
    "dropped" to p.dropped.map(::planItemToMap),
)

private fun planItemToMap(i: PlanItem): Map<String, Any?> = mapOf(
    "type" to i.type, "skill_id" to i.skillId, "misconception_id" to i.misconceptionId,
    "day" to i.day, "minutes" to i.minutes, "status" to i.status, "reason" to i.reason,
)

private fun planFromMap(m: Map<String, Any?>): Plan {
    val items = asList(m["items"] ?: emptyList<Any?>(), "plan.items").mapIndexed { i, x -> planItemFromMap(asMap(x, "plan.items[$i]")) }
    val dropped = asList(m["dropped"] ?: emptyList<Any?>(), "plan.dropped").mapIndexed { i, x -> planItemFromMap(asMap(x, "plan.dropped[$i]")) }
    return Plan(
        version = int(m["version"], "plan.version"),
        createdDay = num(m["created_day"], "plan.created_day"),
        reason = (m["reason"] as? String) ?: "",
        items = items.toMutableList(),
        dropped = dropped,
    )
}

private fun planItemFromMap(m: Map<String, Any?>): PlanItem {
    val type = str(m["type"], "plan_item.type")
    return PlanItem(
        type = type,
        skillId = m["skill_id"] as? String,
        misconceptionId = m["misconception_id"] as? String,
        day = (m["day"] as? Number)?.toInt(),
        minutes = int(m["minutes"] ?: 0, "plan_item.minutes"),
        status = (m["status"] as? String) ?: "planned",
        reason = (m["reason"] as? String) ?: "",
    )
}
