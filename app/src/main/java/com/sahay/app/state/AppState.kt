package com.sahay.app.state

import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sahay.app.data.SnapshotStore
import com.sahay.app.llm.SahayLlm
import com.sahay.engine.AttemptRow
import com.sahay.engine.CalibrationTracker
import com.sahay.engine.ChatMessage
import com.sahay.engine.Content
import com.sahay.engine.Diagnostic
import com.sahay.engine.Fallbacks
import com.sahay.engine.FatigueMonitor
import com.sahay.engine.GoalForecast
import com.sahay.engine.GovMode
import com.sahay.engine.LearnerModel
import com.sahay.engine.LearnerProfileRow
import com.sahay.engine.LlmResult
import com.sahay.engine.MisconceptionEngine
import com.sahay.engine.Plan
import com.sahay.engine.PlanItem
import com.sahay.engine.Question
import com.sahay.engine.ReplayState
import com.sahay.engine.SessionRow
import com.sahay.engine.ShareCode
import com.sahay.engine.Snapshot
import com.sahay.engine.TeachBackRow
import com.sahay.engine.applyAnswer
import com.sahay.engine.buildPlan
import com.sahay.engine.forecastToTarget
import com.sahay.engine.heatmap
import com.sahay.engine.recover
import com.sahay.engine.replay
import com.sahay.engine.validateAttempts
import com.sahay.engine.validationErrors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

enum class Screen { Welcome, Intake, Diagnostic, Today, Study, Roadmap, Tutor, Dashboard, Recover, Device, Settings, Scan, Share }

data class DiagnosticFeedback(val correct: Boolean, val misconception: String?, val correctAnswer: String, val skillName: String)

/** One line in the tutor chat. [template] marks text built from the skill note because the model file is absent. */
data class ChatLine(val from: String, val text: String, val streaming: Boolean = false, val template: Boolean = false)

/** Result of the last check question on the Study screen. */
data class CheckFeedback(val correct: Boolean, val misconception: String?, val correctAnswer: String)

/** One reading of the device signals that feed the governor (B13). */
data class DeviceSnapshot(
    val mode: GovMode,
    val thermal: Int,
    val batteryPct: Int,
    val charging: Boolean,
    val freeMemMb: Int,
    val maxNewTokens: Int,
    val threads: Int,
    val contextTokens: Int,
    val deferNonUrgent: Boolean,
    val cachedOnly: Boolean,
)

/** One skill line of the merged class heatmap (B17). */
data class HeatRow(val skillId: String, val name: String, val students: Int, val meanMastery: Double)

/**
 * Single application-scoped state holder (issue B03 shell, B04 store, B05 to B13 screens).
 *
 * It lives in [com.sahay.app.SahayApplication] so the selected language, the diagnostic in progress and every screen answer
 * survive configuration changes. All learner facts are written to [SnapshotStore]; the engine objects remain the single
 * implementation of every learning decision.
 */
class AppState(private val app: Application) {
    private val store = SnapshotStore(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val contents = HashMap<String, Content>()
    private val storageQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init { scope.launch(Dispatchers.IO) { for (operation in storageQueue) operation() } }
    var restoring by mutableStateOf(true)
        private set
    private var taskEpoch = 0


    var screen by mutableStateOf(Screen.Welcome)
        private set
    var language by mutableStateOf("en")
        private set
    var domain by mutableStateOf("dsa")
        private set
    var targets by mutableStateOf(DOMAIN_TARGETS.getValue("dsa"))
        private set

    var profile by mutableStateOf<LearnerProfileRow?>(null)
        private set
    var model by mutableStateOf(LearnerModel())
        private set
    var misconceptions by mutableStateOf(MisconceptionEngine())
        private set
    var calibration by mutableStateOf(CalibrationTracker())
        private set
    var fatigue by mutableStateOf(FatigueMonitor())
        private set

    var attempts by mutableStateOf<List<AttemptRow>>(emptyList())
        private set
    var teachBacks by mutableStateOf<List<TeachBackRow>>(emptyList())
        private set
    var sessions by mutableStateOf<List<SessionRow>>(emptyList())
        private set
    var plan by mutableStateOf<Plan?>(null)
        private set
    var planChangeNote by mutableStateOf("")
        private set

    /** Bumped after a mutation of the engine objects (they are plain classes) so Compose redraws derived values. */
    var revision by mutableStateOf(0)
        private set
    var storageError by mutableStateOf<String?>(null)
        private set

    private var localizedFor = ""
    private var localized: Resources? = null

    // Diagnostic session.
    var diagnostic by mutableStateOf<Diagnostic?>(null)
        private set
    var question by mutableStateOf<Question?>(null)
        private set
    var pendingChoice by mutableStateOf<Int?>(null)
        private set
    var confidence by mutableStateOf(50.0)
        private set
    var hintLevel by mutableStateOf<Int?>(null)
        private set
    var hintText by mutableStateOf("")
        private set
    var hintLoading by mutableStateOf(false)
        private set
    var hintTemplate by mutableStateOf(false)
        private set
    var diagnosticFeedback by mutableStateOf<DiagnosticFeedback?>(null)
        private set
    var diagnosticAnswerCount by mutableStateOf(0)
        private set
    private var questionShownAt = SystemClock.elapsedRealtime()

    // Study screen.
    var studySkillId by mutableStateOf<String?>(null)
        private set
    var studyPractice by mutableStateOf(false)
        private set
    var studyText by mutableStateOf("")
        private set
    var studyAnalogy by mutableStateOf("")
        private set
    var studyCheck by mutableStateOf("")
        private set
    var studySource by mutableStateOf("")
        private set
    var studyLoading by mutableStateOf(false)
        private set
    var studyTemplate by mutableStateOf(false)
        private set
    var studyQuestion by mutableStateOf<Question?>(null)
        private set
    var checkFeedback by mutableStateOf<CheckFeedback?>(null)
        private set
    var checkPendingChoice by mutableStateOf<Int?>(null)
        private set
    private var checkShownAt = SystemClock.elapsedRealtime()
    private var studyJob: Job? = null

    // Tutor screen.
    var chat by mutableStateOf<List<ChatLine>>(emptyList())
        private set
    var tutorStreaming by mutableStateOf(false)
        private set
    var tutorSkillId by mutableStateOf<String?>(null)
        private set
    private var tutorJob: Job? = null
    private var tutorTurn = 0

    /**
     * True only when an ExecuTorch runtime is compiled in *and* a model file is present. Any generated text is reported as
     * template text otherwise, so the UI never implies a model result that did not happen.
     */
    val modelPresent: Boolean get() = SahayLlm.ready && SahayLlm.runtimeAvailable &&
        File(app.filesDir, "models/${SahayLlm.MODEL_FILE}").isFile

    val llmLanguage: String get() = language

    val day: Double
        get() {
            val p = profile ?: return 0.0
            return (nowDays() - p.createdAt).coerceAtLeast(0.0)
        }

    // ---- lifecycle ----

    /** Load the saved snapshot and rebuild learner state by replaying the attempt log. */
    fun restore() {
        storageQueue.trySend {
            val snap = store.load()
            withContext(Dispatchers.Main) {
                if (snap != null) applySnapshot(snap)
                storageError = store.lastError
                restoring = false
            }
        }
    }

    private fun applySnapshot(snap: Snapshot) {
        try {
            language = snap.language
            profile = snap.profile
            domain = snap.profile?.domain ?: "dsa"
            val known = contentFor(domain).graph.skills.keys
            targets = snap.targets.filter { it in known }.ifEmpty { DOMAIN_TARGETS.getValue(domain) }
            attempts = snap.attempts
            teachBacks = snap.teachBacks
            sessions = snap.sessions
            plan = snap.plan
            planChangeNote = snap.planChangeNote
            val r = replay(contentFor(domain), targets, attempts)
            adopt(r)
            screen = if (snap.profile == null) Screen.Welcome else Screen.Today
        } catch (e: Exception) {
            storageError = e.message ?: e.javaClass.simpleName
            screen = Screen.Welcome
        }
    }

    private fun adopt(r: ReplayState) {
        model = r.model
        misconceptions = r.misconceptions
        calibration = r.calibration
        fatigue = r.fatigue
        revision++
    }

    fun contentFor(d: String): Content = contents.getOrPut(d) {
        val graph = app.assets.open("skillgraphs/$d.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val bank = app.assets.open("questions/$d.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        Content.fromJson(graph, bank)
    }

    /** Resources for the selected UI language, used for the few strings the state holder formats itself. */
    fun resources(): Resources {
        if (localizedFor == language && localized != null) return localized!!
        val config = Configuration(app.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        localized = app.createConfigurationContext(config).resources
        localizedFor = language
        return localized!!
    }

    private fun str(id: Int, vararg args: Any): String = resources().getString(id, *args)

    fun go(next: Screen) { screen = next }

    fun selectLanguage(code: String) {
        require(code in setOf("en", "hi", "ta", "te", "bn", "mr"))
        if (code == language) return
        language = code
        persist()
    }

    // ---- intake (B05) ----

    fun startIntake() { screen = Screen.Intake }

    /** Map a free-text goal to a domain, or null so the UI falls back to domain chips. */
    fun domainFromGoal(text: String): String? {
        val t = text.lowercase()
        fun hit(words: List<String>) = words.any { t.contains(it) }
        val dsa = hit(listOf("dsa", "data structure", "algorithm", "placement", "interview", "coding", "leetcode"))
        val ml = hit(listOf("machine learning", " ml", "ml ", "data science", "artificial intelligence", "deep learning", "ai "))
        return when {
            dsa && !ml -> "dsa"
            ml && !dsa -> "mleng"
            else -> null
        }
    }

    fun createProfile(goal: String, domainId: String, level: String, weeklyHours: Double, weeks: Int, interests: List<String>) {
        domain = domainId
        targets = DOMAIN_TARGETS.getValue(domainId)
        profile = LearnerProfileRow(
            goal = goal.ifBlank { if (domainId == "dsa") "DSA for placements" else "Machine learning engineering" },
            domain = domainId,
            level = level,
            hoursPerWeek = weeklyHours,
            studyDays = 5,
            deadlineDay = (weeks * 7).coerceIn(7, 365),
            targetMastery = 0.8,
            interests = interests,
            language = language,
            createdAt = nowDays(),
        )
        plan = null
        planChangeNote = ""
        attempts = emptyList()
        teachBacks = emptyList()
        sessions = emptyList()
        adopt(ReplayState(LearnerModel(), MisconceptionEngine(), CalibrationTracker(), FatigueMonitor()))
        persist()
        startDiagnostic()
    }

    // ---- diagnostic (B07) ----

    fun startDiagnostic() {
        taskEpoch++
        val p = profile ?: return
        try {
            diagnostic = Diagnostic(contentFor(domain), model, misconceptions, targets, now = day)
        } catch (e: Exception) {
            storageError = e.message ?: e.javaClass.simpleName
            return
        }
        diagnosticAnswerCount = 0
        diagnosticFeedback = null
        pendingChoice = null
        confidence = 50.0
        hintLevel = null
        hintText = ""
        hintTemplate = false
        question = diagnostic?.nextQuestion()
        questionShownAt = SystemClock.elapsedRealtime()
        screen = Screen.Diagnostic
    }

    fun chooseOption(index: Int) { if (diagnosticFeedback == null) pendingChoice = index }

    fun updateConfidence(value: Double) { confidence = value.coerceIn(0.0, 100.0) }

    fun requestHint() {
        val q = question ?: return
        val level = ((hintLevel ?: -1) + 1).coerceAtMost(3)
        hintLevel = level
        hintText = ""
        hintLoading = true
        val c = contentFor(domain)
        val notes = c.graph.skills[q.skill]?.notes ?: ""
        val epoch = taskEpoch
        scope.launch {
            val inp = mapOf(
                "stem" to q.stem,
                "options" to q.options.map { it.text },
                "learner_answer" to "",
                "level" to level,
                "notes" to notes,
                "_correct_text" to q.options[q.answer].text,
            )
            val r = runTask("hint", inp)
            if (epoch != taskEpoch || question?.id != q.id) return@launch
            hintText = (r.output?.get("hint") as? String) ?: ""
            hintTemplate = r.source != "model"
            hintLoading = false
        }
    }

    fun submitDiagnosticAnswer() {
        if (diagnosticFeedback != null) return
        val q = question ?: return
        val chosen = pendingChoice ?: return
        val d = diagnostic ?: return
        val responseMs = (SystemClock.elapsedRealtime() - questionShownAt).toDouble()
        val level = hintLevel ?: 0
        val correct = d.record(q, chosen, confidence, responseMs, level)
        calibration.add(confidence, correct)
        fatigue.add(responseMs, correct)
        val c = contentFor(domain)
        val mid = q.options.getOrNull(chosen)?.misconception
        attempts = attempts + AttemptRow(
            seq = nextSeq(), questionId = q.id, skillId = q.skill, chosenIndex = chosen, correct = correct,
            confidence = confidence, responseMs = responseMs, hintLevel = level, day = day,
        )
        diagnosticFeedback = DiagnosticFeedback(
            correct = correct,
            misconception = mid?.let { c.misconceptions[it]?.description },
            correctAnswer = q.options[q.answer].text,
            skillName = c.graph.skills[q.skill]?.name ?: q.skill,
        )
        diagnosticAnswerCount = d.history.size
        revision++
        persist()
    }

    fun nextDiagnosticQuestion() {
        taskEpoch++
        val d = diagnostic ?: return
        diagnosticFeedback = null
        pendingChoice = null
        confidence = 50.0
        hintLevel = null
        hintText = ""
        if (d.done()) {
            finishDiagnostic()
            return
        }
        question = d.nextQuestion()
        questionShownAt = SystemClock.elapsedRealtime()
    }

    private fun finishDiagnostic() {
        question = null
        planChangeNote = "Your first plan is based on the diagnostic answers."
        buildRoadmap("Diagnostic placed you. ")
        screen = Screen.Roadmap
    }

    // ---- planning (B08) ----

    fun buildRoadmap(reasonPrefix: String = "") {
        val p = profile ?: return
        val previous = plan
        val next = buildPlan(
            contentFor(domain), model, misconceptions, p.toPlannerProfile(), day, targets,
            version = (previous?.version ?: 0) + 1, reasonPrefix = reasonPrefix,
        )
        plan = next
        revision++
        persist()
    }

    /** One plain sentence for every change of plan (PRD 13.2). */
    private fun replanAfterAnswer(skillId: String, correct: Boolean, misconception: String?) {
        val p = profile ?: return
        val c = contentFor(domain)
        val name = c.graph.skills[skillId]?.name ?: skillId
        val reason = when {
            misconception != null -> "A repair item was added for %s because a confident answer showed that misconception."
                .format(misconception)
            !correct -> "%s was given more time because you answered a question on it wrongly.".format(name)
            else -> "Your plan was refreshed after a correct answer on %s.".format(name)
        }
        val next = buildPlan(
            c, model, misconceptions, p.toPlannerProfile(), day, targets,
            version = (plan?.version ?: 0) + 1, reasonPrefix = reason,
        )
        plan = next
        planChangeNote = reason
        revision++
        persist()
    }

    fun nextPlanItem(): PlanItem? = plan?.items?.firstOrNull { it.status == "planned" }

    fun planItemsFor(skillId: String): List<PlanItem> = plan?.items?.filter { it.skillId == skillId } ?: emptyList()

    // ---- study and practice (B09) ----

    private fun nextSeq(): Int = (attempts.maxOfOrNull { it.seq } ?: 0) + 1

    fun openStudy(skillId: String? = null, practice: Boolean = false) {
        taskEpoch++
        val id = skillId ?: nextPlanItem()?.skillId ?: run {
            screen = Screen.Today
            return
        }
        studySkillId = id
        studyPractice = practice
        studyText = ""
        studyAnalogy = ""
        studyCheck = ""
        studySource = ""
        studyTemplate = false
        studyLoading = true
        checkFeedback = null
        checkPendingChoice = null
        studyQuestion = pickQuestion(id)
        checkShownAt = SystemClock.elapsedRealtime()
        screen = Screen.Study
        studyJob?.cancel()
        studyJob = scope.launch { loadExplanation(id, false) }
    }

    fun requestAnalogy() {
        val id = studySkillId ?: return
        studyJob?.cancel()
        studyJob = scope.launch { loadExplanation(id, true) }
    }

    private suspend fun loadExplanation(skillId: String, withAnalogy: Boolean) {
        studyLoading = true
        val c = contentFor(domain)
        val skill = c.graph.skills[skillId] ?: run { studyLoading = false; return }
        val interests = profile?.interests ?: emptyList()
        val inp = mapOf(
            "skill_id" to skill.id,
            "skill_name" to skill.name,
            "notes" to skill.notes,
            "question" to (studyQuestion?.stem ?: ""),
            "interests" to interests,
            "analogy" to withAnalogy,
            "language" to llmLanguage,
        )
        val epoch = taskEpoch
        val r = runTask("explain", inp) { partial ->
            scope.launch {
                if (epoch == taskEpoch && studySkillId == skillId && studyLoading) studyText = partial
            }
        }
        if (epoch != taskEpoch || studySkillId != skillId) return
        studyText = (r.output?.get("explanation") as? String) ?: skill.notes
        studyAnalogy = (r.output?.get("analogy") as? String) ?: ""
        studyCheck = (r.output?.get("check_question") as? String) ?: ""
        studySource = skill.notes
        studyTemplate = r.source != "model"
        studyLoading = false
    }

    private fun pickQuestion(skillId: String): Question? {
        val all = contentFor(domain).bySkill[skillId] ?: return null
        val seen = attempts.filter { it.skillId == skillId }.map { it.questionId }.toSet()
        return all.firstOrNull { it.id !in seen } ?: all.firstOrNull()
    }

    fun chooseCheckOption(index: Int) { if (checkFeedback == null) checkPendingChoice = index }

    fun submitCheckAnswer() {
        if (checkFeedback != null) return
        val q = studyQuestion ?: return
        val chosen = checkPendingChoice ?: return
        val c = contentFor(domain)
        val responseMs = (SystemClock.elapsedRealtime() - checkShownAt).toDouble()
        val closure = c.graph.closure(targets).sorted()
        // The check question has no confidence slider in this screen, so calibration is not updated (that is B07's job).
        val correct = applyAnswer(c, model, misconceptions, closure, q, chosen, 50.0, responseMs, 0, day)
        fatigue.add(responseMs, correct)
        attempts = attempts + AttemptRow(
            seq = nextSeq(), questionId = q.id, skillId = q.skill, chosenIndex = chosen, correct = correct,
            confidence = 50.0, responseMs = responseMs, hintLevel = 0, day = day, confidenceRated = false,
        )
        val mid = q.options.getOrNull(chosen)?.misconception
        checkFeedback = CheckFeedback(
            correct = correct,
            misconception = mid?.let { c.misconceptions[it]?.description },
            correctAnswer = q.options[q.answer].text,
        )
        plan?.items?.firstOrNull { it.status == "planned" && it.skillId == q.skill }?.let { it.status = "done" }
        revision++
        persist()
        if (!correct || checkFeedback?.misconception != null) replanAfterAnswer(q.skill, correct, checkFeedback?.misconception)
    }

    fun nextCheckQuestion() {
        val id = studySkillId ?: return
        studyQuestion = pickQuestion(id)
        checkFeedback = null
        checkPendingChoice = null
        checkShownAt = SystemClock.elapsedRealtime()
    }

    // ---- tutor (B06) ----

    fun openTutor(skillId: String? = null) {
        tutorSkillId = skillId ?: studySkillId ?: nextPlanItem()?.skillId ?: targets.firstOrNull()
        screen = Screen.Tutor
    }

    fun contextSkillName(): String {
        val id = tutorSkillId ?: return ""
        return contentFor(domain).graph.skills[id]?.name ?: ""
    }

    fun contextSkillNotes(): String {
        val id = tutorSkillId ?: return ""
        return contentFor(domain).graph.skills[id]?.notes ?: ""
    }

    fun sendTutor(text: String) {
        val message = text.trim()
        if (message.isEmpty() || tutorStreaming) return
        val id = tutorSkillId ?: nextPlanItem()?.skillId ?: targets.firstOrNull()
        tutorSkillId = id
        chat = chat + ChatLine(from = "you", text = message)
        if (id == null) {
            chat = chat + ChatLine(from = "sahay", text = str(com.sahay.app.R.string.tutor_empty), template = true)
            return
        }
        val skill = contentFor(domain).graph.skills[id]
        if (!modelPresent) {
            val note = skill?.notes ?: ""
            val head = note.split(Regex("(?<=[.!?])\\s+")).take(3).joinToString(" ")
            chat = chat + ChatLine(
                from = "sahay",
                text = "$head\n\n${str(com.sahay.app.R.string.tutor_model_missing)}",
                template = true,
            )
            return
        }
        chat = chat + ChatLine(from = "sahay", text = "", streaming = true)
        tutorStreaming = true
        val prompt = tutorPrompt(skill?.name ?: id, skill?.notes ?: "", message)
        val turn = ++tutorTurn
        tutorJob = scope.launch {
            try {
                SahayLlm.stream(prompt, 220, 0.7).collect { token -> if (turn == tutorTurn) appendTutor(token) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (turn == tutorTurn) appendTutor("\n" + str(com.sahay.app.R.string.tutor_failed, e.message ?: "error"))
            } finally {
                if (turn == tutorTurn) finishTutorStream()
            }
        }
    }

    fun stopTutor() {
        if (!tutorStreaming) return
        tutorTurn++
        tutorJob?.cancel()
        tutorJob = null
        appendTutor("\n" + str(com.sahay.app.R.string.tutor_stopped))
        finishTutorStream()
    }

    private fun appendTutor(token: String) {
        val list = chat.toMutableList()
        if (list.isEmpty()) return
        val last = list.last()
        if (last.from != "sahay") return
        list[list.lastIndex] = last.copy(text = last.text + token)
        chat = list
    }

    private fun finishTutorStream() {
        tutorStreaming = false
        val list = chat.toMutableList()
        if (list.isNotEmpty() && list.last().from == "sahay") list[list.lastIndex] = list.last().copy(streaming = false)
        chat = list
    }

    private fun tutorPrompt(skillName: String, notes: String, question: String): String {
        val lang = Locale.forLanguageTag(llmLanguage).getDisplayLanguage(Locale.ENGLISH)
        val system = "You are Sahay, an offline study tutor. Stay on the topic of $skillName. Use these notes: $notes " +
            "Do not give medical, legal or financial advice. Answer in $lang in under 120 words."
        val messages = listOf(ChatMessage("system", system), ChatMessage("user", question))
        return "<|begin_of_text|>" + messages.joinToString("") {
            "<|start_header_id|>${it.role}<|end_header_id|>\n\n${it.content}<|eot_id|>"
        } + "<|start_header_id|>assistant<|end_header_id|>\n\n"
    }

    // ---- LLM task plumbing ----

    fun stopStudy() {
        studyJob?.cancel()
        studyJob = null
        studyLoading = false
        studyText = studySource.ifBlank { studySkillId?.let { contentFor(domain).graph.skills[it]?.notes }.orEmpty() }
        studyTemplate = true
    }

    private suspend fun runTask(task: String, input: Map<String, Any?>, onPartial: ((String) -> Unit)? = null): LlmResult = withContext(Dispatchers.IO) {
        if (SahayLlm.ready && task == "explain" && onPartial != null) {
            val job = coroutineContext[Job]
            SahayLlm.explain(input, llmLanguage, { job?.isActive == false }, onPartial)
        } else if (SahayLlm.ready) {
            SahayLlm.orchestrator.run(task, input, llmLanguage)
        } else {
            LlmResult(task, Fallbacks.make(task, input, llmLanguage), "fallback", 0, listOf("runtime not initialised"))
        }
    }

    /** OCR is untrusted data. The prompt builder escapes delimiters and the orchestrator validates every result. */
    fun runScannedTask(task: String, text: String, onComplete: (LlmResult) -> Unit) {
        require(task in setOf("draft_question", "syllabus_compile"))
        val clean = text.replace(Regex("[\\p{Cc}&&[^\\n\\t]]"), " ").take(6000)
        val input = if (task == "syllabus_compile") mapOf("ocr_text" to clean)
            else mapOf("skill_id" to (studySkillId ?: targets.firstOrNull().orEmpty()), "notes" to clean)
        val epoch = taskEpoch
        scope.launch {
            val result = runTask(task, input)
            if (epoch == taskEpoch) onComplete(result)
        }
    }

    // ---- dashboard (B10) ----

    /** Skills in the goal closure in prerequisite order, for the mastery heatmap. */
    fun goalSkills(): List<String> = runCatching {
        val c = contentFor(domain)
        c.graph.topoOrder(c.graph.closure(targets))
    }.getOrElse { emptyList() }

    /** Projection to the target, or null before a profile exists. Never invents a number: see [ForecastBasis]. */
    fun forecast(): GoalForecast? {
        val p = profile ?: return null
        return runCatching {
            forecastToTarget(contentFor(domain), targets, attempts, day, p.targetMastery)
        }.getOrNull()
    }

    fun answersFor(skillId: String): Int = attempts.count { it.skillId == skillId }

    fun needsRevision(skillId: String): Boolean = model.needsRevision(skillId, day)

    // ---- recovery (B11) ----

    /** Re-pack the remaining work into [budgetMinutes] with the engine's knapsack and list every dropped skill. */
    fun recoverPlan(budgetMinutes: Int) {
        val p = profile ?: return
        val next = runCatching {
            recover(
                contentFor(domain), model, p.toPlannerProfile(), day, targets,
                oldPlan = plan, budgetMinutes = budgetMinutes, mis = misconceptions,
                version = (plan?.version ?: 0) + 1,
            )
        }.getOrNull() ?: return
        if (next.items.isEmpty() && next.dropped.isEmpty()) {
            planChangeNote = "Recovery found nothing left below the target mastery."
            revision++
            persist()
            return
        }
        plan = next
        planChangeNote = "Recovery re-packed the remaining skills into $budgetMinutes minutes."
        revision++
        persist()
        screen = Screen.Recover
    }

    // ---- derived values for today, roadmap and dashboard ----

    fun mastery(skillId: String): Double = model.pEff(skillId, day)

    fun averageMastery(): Double {
        val ids = runCatching { contentFor(domain).graph.closure(targets) }.getOrNull() ?: return 0.0
        if (ids.isEmpty()) return 0.0
        return ids.sumOf { model.pEff(it, day) } / ids.size
    }

    fun streakDays(): Int {
        if (attempts.isEmpty()) return 0
        val days = attempts.map { floor(it.day).toInt() }.distinct().sortedDescending()
        var want = floor(day).toInt()
        var streak = 0
        for (d in days) {
            if (d == want) { streak++; want-- } else if (d < want) break
        }
        return streak
    }

    fun activeMisconceptions(): List<String> = misconceptions.active()

    fun misconceptionText(id: String): String = contentFor(domain).misconceptions[id]?.description ?: id

    // ---- settings and export (B13) ----

    /** Last device sample, or null before the monitor has run (or when the LLM layer failed to initialise). */
    fun deviceSnapshot(): DeviceSnapshot? {
        if (!SahayLlm.ready) return null
        val s = SahayLlm.monitor.last ?: return null
        val l = SahayLlm.governor.limits()
        return DeviceSnapshot(
            mode = SahayLlm.governor.mode,
            thermal = s.thermal, batteryPct = s.batteryPct, charging = s.charging, freeMemMb = s.freeMemMb,
            maxNewTokens = l.maxNewTokens, threads = l.threads, contextTokens = l.contextTokens,
            deferNonUrgent = l.deferNonUrgent, cachedOnly = l.cachedOnly,
        )
    }

    /** Small, honest summary for the settings export card. */
    fun exportSummary(): String =
        "${attempts.size} attempts, ${teachBacks.size} teach-backs, ${sessions.size} sessions, " +
            "${plan?.items?.size ?: 0} plan items"

    /**
     * Replace local state with an exported snapshot. Returns null on success or a readable reason when the snapshot is
     * malformed, belongs to another domain, or references questions this build does not bundle.
     */
    fun importSnapshot(text: String, onComplete: (String?) -> Unit) {
        val snap = try { if (text.trim().startsWith(ShareCode.PREFIX)) ShareCode.decode(text) else Snapshot.fromJson(text) } catch (e: Exception) { onComplete(e.message ?: "invalid snapshot"); return }
        val p = snap.profile ?: run { onComplete("the snapshot has no learner profile"); return }
        if (p.domain !in DOMAIN_TARGETS) { onComplete("unknown domain '${p.domain}'"); return }
        val errors = snap.validationErrors(contentFor(p.domain))
        if (errors.isNotEmpty()) { onComplete(errors.first()); return }
        stopTutor()
        studyJob?.cancel()
        taskEpoch++
        restoring = true
        val json = snap.toJson()
        storageQueue.trySend {
            val saved = store.saveJson(json)
            withContext(Dispatchers.Main) {
                if (saved) { storageError = null; applySnapshot(snap) }
                restoring = false
                onComplete(if (saved) null else store.lastError ?: "could not save snapshot")
            }
        }
    }

    /**
     * Merge student share codes into a class heatmap (B17). Every code is decoded and checked first, and all students
     * must share one domain because the teacher phone replays each attempt log against one content pack. Returns the
     * rows plus a readable reason when the input was rejected.
     */
    fun classHeatmap(codes: List<String>): Pair<List<HeatRow>, String?> {
        val snaps = ArrayList<Snapshot>()
        var domain: String? = null
        codes.forEachIndexed { index, raw ->
            val snap = try {
                ShareCode.decode(raw)
            } catch (e: Exception) {
                return emptyList<HeatRow>() to "code ${index + 1}: ${e.message ?: "unreadable"}"
            }
            val p = snap.profile ?: return emptyList<HeatRow>() to "code ${index + 1} has no learner profile"
            if (p.domain !in DOMAIN_TARGETS) return emptyList<HeatRow>() to "code ${index + 1} uses an unknown domain"
            if (domain == null) domain = p.domain
            else if (domain != p.domain) {
                return emptyList<HeatRow>() to "codes mix domains ($domain and ${p.domain})"
            }
            val errors = snap.validationErrors(contentFor(p.domain))
            if (errors.isNotEmpty()) return emptyList<HeatRow>() to "code ${index + 1}: ${errors.first()}"
            snaps.add(snap)
        }
        if (snaps.isEmpty()) return emptyList<HeatRow>() to null
        val rows = heatmap(contentFor(domain!!), snaps).map {
            HeatRow(it.skillId, it.name, it.students, it.meanMastery)
        }
        return rows to null
    }

    fun snapshot(): Snapshot = Snapshot(
        profile = profile,
        targets = targets,
        attempts = attempts,
        teachBacks = teachBacks,
        sessions = sessions,
        plan = plan,
        planChangeNote = planChangeNote,
        language = language,
    )

    fun exportJson(): String = snapshot().toJson()

    fun deleteAllData() {
        stopTutor()
        studyJob?.cancel()
        taskEpoch++
        restoring = true
        storageQueue.trySend {
            if (SahayLlm.ready) SahayLlm.manager.release()
            val deleted = store.deleteAll()
            withContext(Dispatchers.Main) {
                if (deleted) clearLearnerState()
                else storageError = store.lastError
                restoring = false
            }
        }
    }

    private fun clearLearnerState() {
        profile = null
        attempts = emptyList()
        teachBacks = emptyList()
        sessions = emptyList()
        plan = null
        planChangeNote = ""
        diagnostic = null
        question = null
        studySkillId = null
        chat = emptyList()
        language = "en"
        domain = "dsa"
        targets = DOMAIN_TARGETS.getValue("dsa")
        adopt(ReplayState(LearnerModel(), MisconceptionEngine(), CalibrationTracker(), FatigueMonitor()))
        screen = Screen.Welcome
    }

    fun storageErrorText(): String? = storageError ?: store.lastError

    private fun persist() {
        if (restoring) return
        // Serialise on the caller's thread (usually main) so a mutable Plan cannot change while the writer walks it.
        val json = snapshot().toJson()
        storageQueue.trySend {
            val saved = store.saveJson(json)
            withContext(Dispatchers.Main) { storageError = if (saved) null else store.lastError }
        }
    }

    private fun nowDays(): Double = System.currentTimeMillis() / 86_400_000.0

    companion object {
        /** Small curated target sets so the first roadmap is focused; both are validated leaf skills of the bundled graphs. */
        val DOMAIN_TARGETS: Map<String, List<String>> = mapOf(
            "dsa" to listOf("dsa.topo", "dsa.dp_1d"),
            "mleng" to listOf("ml.svm", "ml.clustering"),
        )

        fun percent(value: Double): Int = round(value * 100).toInt()
    }
}
