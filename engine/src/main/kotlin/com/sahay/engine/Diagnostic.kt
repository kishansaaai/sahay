package com.sahay.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Adaptive diagnostic (PRD 6 C3): 10 to 15 questions chosen to resolve the most uncertainty about the goal skills.
 * Port of diagnostic.py.
 */
class Diagnostic(
    private val c: Content,
    private val model: LearnerModel,
    private val mis: MisconceptionEngine,
    targets: Collection<String>,
    seedDifficulty: Int = 2,
    private val now: Double = 0.0,
) {
    private val closure: List<String> = c.graph.closure(targets).sorted()
    private val askedSkills = HashSet<String>()
    private val askedQ = HashSet<String>()
    val history = ArrayList<Pair<String, Boolean>>()
    private var level = seedDifficulty

    private fun uncertainty(s: String): Double { val p = model.state(s).p; return p * (1 - p) }

    fun done(): Boolean {
        val n = history.size
        if (n >= MAX_Q) return true
        if (n < MIN_Q) return false
        val left = closure.filter { it !in askedSkills }
        return left.isEmpty() || left.maxOf { uncertainty(it) } < UNCERTAINTY_STOP
    }

    fun nextQuestion(): Question? {
        if (done()) return null
        val g = c.graph
        val cands = closure.filter { it !in askedSkills }.ifEmpty { closure }
        val s = cands.maxWithOrNull(
            compareBy<String> { round(uncertainty(it) * 1e6) / 1e6 }.thenBy { g.dependents(it).size }.thenBy { it })!!
        val all = c.bySkill.getValue(s)
        val qs = all.filter { it.id !in askedQ }.ifEmpty { all }
        return qs.minWithOrNull(compareBy<Question> { Math.abs(it.difficulty - level) }.thenBy { it.id })
    }

    fun record(q: Question, chosenIndex: Int, confidence: Double, responseMs: Double): Boolean {
        val g = c.graph
        val correct = q.options[chosenIndex].misconception == null
        model.observe(q.skill, correct, confidence, responseMs, 0, now, nOptions = q.options.size)
        mis.observe(q, chosenIndex, confidence, now)
        askedSkills.add(q.skill)
        askedQ.add(q.id)
        history.add(q.id to correct)
        if (correct) {
            level = min(5, if (q.difficulty >= level) level + 1 else level)
            for (p in g.prereqClosure(q.skill)) {
                val st = model.state(p)
                st.p = max(st.p, INFER_FLOOR)
            }
        } else {
            level = max(1, level - 1)
            val sp = model.state(q.skill).p
            for (d in closure) {
                if (q.skill in g.prereqClosure(d)) {
                    val st = model.state(d)
                    st.p = min(st.p, max(sp, model.params.p0))
                }
            }
        }
        return correct
    }

    companion object {
        const val MIN_Q = 10
        const val MAX_Q = 15
        const val INFER_FLOOR = 0.6         // ASSUMPTION: implied mastery for prerequisites of a skill answered correctly
        const val UNCERTAINTY_STOP = 0.16   // ASSUMPTION: stop after MIN_Q when no skill is more uncertain than this
    }
}
