package com.sahay.engine

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Planner and re-planner (PRD 9.5 and 9.6, feature C5). Port of planner.py.
 * The LLM only narrates a plan, it never changes the order. Day numbers are integers (absolute).
 */
const val SESSION_MIN = 30        // ASSUMPTION: maximum length of one study session chunk
const val REPAIR_MIN = 15
const val REVISE_MIN = 10
const val MIN_LEARN_MIN = 20
const val ASSUMED_GROWTH = 2.0    // stability multiplier assumed for a planned learn or revise

data class Profile(
    val hoursPerWeek: Double = 6.0,
    val studyDays: Int = 5,
    val deadlineDay: Int = 70,
    val targetMastery: Double = 0.8,
    val weights: Map<String, Double> = emptyMap(),
) {
    val dayMinutes: Int get() = round(hoursPerWeek * 60.0 / studyDays).toInt()
    fun isStudyDay(day: Int, start: Int): Boolean = Math.floorMod(day - start, 7) < studyDays
}

class PlanItem(
    val type: String,                 // learn | practice | revise | repair
    val skillId: String? = null,
    val misconceptionId: String? = null,
    var day: Int? = null,
    val minutes: Int = 0,
    var status: String = "planned",   // planned | done | skipped | dropped
    var reason: String = "",
)

class Plan(val version: Int, val createdDay: Double, var reason: String, val items: MutableList<PlanItem>, var dropped: List<PlanItem> = emptyList()) {
    fun weeks(start: Int): Map<Int, List<PlanItem>> = items.groupBy { Math.floorDiv(it.day!! - start, 7) }

    fun learnOrder(): List<String> = items.filter { it.type == "learn" }.map { it.skillId!! }.distinct()

    /** Planned learn skills whose first session is not after the last session of every planned DIRECT prerequisite. */
    fun prerequisiteViolations(content: Content, mastered: Set<String> = emptySet()): Int {
        val first = HashMap<String, Int>()
        val last = HashMap<String, Int>()
        items.forEachIndexed { i, it ->
            if (it.type == "learn") { first.putIfAbsent(it.skillId!!, i); last[it.skillId] = i }
        }
        var bad = 0
        for ((s, f) in first) {
            for (p in content.graph.prereqs(s)) {
                if (p in mastered) continue
                val l = last[p]
                if (l != null && l > f) { bad++; break }
            }
        }
        return bad
    }
}

/** Minutes the planner allots to learn a skill whose mastery gap is [gap] (shared with recovery). */
fun learnMinutes(content: Content, skill: String, gap: Double, target: Double): Int =
    max(MIN_LEARN_MIN, round(content.graph.estHours(skill) * 60 * min(1.0, gap / target)).toInt())

private fun chunks(total: Int, cap: Int = SESSION_MIN): List<Int> {
    val out = ArrayList<Int>()
    var t = total
    while (t > 0) { val c = min(cap, t); out.add(c); t -= c }
    return out
}

private fun typeRank(t: String) = when (t) { "repair" -> 0; "learn" -> 1; "revise" -> 2; else -> 3 }

fun buildPlan(
    content: Content, model: LearnerModel, mis: MisconceptionEngine, profile: Profile, now: Double, targets: Collection<String>,
    version: Int = 1, useForgetting: Boolean = true, useMisconceptions: Boolean = true, useRevision: Boolean = true,
    reasonPrefix: String = "",
): Plan {
    val g = content.graph
    val tgt = profile.targetMastery
    val closure = g.closure(targets)
    val cur = HashMap<String, Double>()
    for (s in closure) cur[s] = if (useForgetting) model.pEff(s, now) else model.state(s).p
    val todo = closure.filter { tgt - cur.getValue(it) > 1e-9 }.toSet()

    fun need(s: String) = (tgt - cur.getValue(s)) * (profile.weights[s] ?: 1.0) / g.estHours(s)

    // 1-3. topological order with highest-need-first among ready skills
    val order = ArrayList<String>()
    val placed = HashSet<String>()
    val remaining = todo.toMutableSet()
    while (remaining.isNotEmpty()) {
        val ready = remaining.filter { s -> g.prereqs(s).all { it !in todo || it in placed } }
        val s = ready.maxWithOrNull(compareBy<String> { need(it) }.thenBy { it })!!
        remaining.remove(s); placed.add(s); order.add(s)
    }

    val seq = ArrayList<PlanItem>()
    for (s in order) {
        val minutes = learnMinutes(content, s, tgt - cur.getValue(s), tgt)
        for (c in chunks(minutes)) {
            seq.add(PlanItem("learn", skillId = s, minutes = c, reason = "gap %.2f, need %.3f".format(tgt - cur.getValue(s), need(s))))
        }
    }

    // 4. repair items directly before the skill they unblock
    if (useMisconceptions) {
        for (mid in mis.repairs()) {
            val ms = content.misconceptions.getValue(mid).skill
            if (ms !in closure && closure.none { ms in g.prereqClosure(it) }) continue
            var pos = seq.indexOfFirst { it.skillId == ms || ms in g.prereqClosure(it.skillId!!) }
            if (pos < 0) pos = seq.size
            seq.add(pos, PlanItem("repair", skillId = ms, misconceptionId = mid, minutes = REPAIR_MIN,
                reason = "two confident wrong answers on this misconception"))
        }
    }

    // 6. interleave: at most two consecutive sessions of the same skill
    var i = 2
    while (i < seq.size) {
        if (seq[i].skillId == seq[i - 1].skillId && seq[i - 1].skillId == seq[i - 2].skillId) {
            for (j in i + 1 until seq.size) {
                val sj = seq[j].skillId!!
                if (sj == seq[i].skillId) continue
                val between = seq.subList(i, j).map { it.skillId }.toSet()
                if (seq[j].type == "learn" && between.any { it in g.prereqClosure(sj) }) continue
                seq.add(i, seq.removeAt(j))
                break
            }
        }
        i++
    }

    // day assignment within the stated minutes per day
    val start = ceil(now).toInt() + 1
    val capDay = profile.dayMinutes
    val used = HashMap<Int, Int>()
    val items = ArrayList<PlanItem>()
    val dropped = ArrayList<PlanItem>()

    fun nextStudyDay(d0: Int): Int { var d = d0; while (!profile.isStudyDay(d, start)) d++; return d }

    var day = nextStudyDay(start)
    for (it in seq) {
        while ((used[day] ?: 0) + it.minutes > capDay) {
            day = nextStudyDay(day + 1)
            if (day > profile.deadlineDay) break
        }
        if (day > profile.deadlineDay) {
            it.status = "dropped"; it.reason = "not enough hours before the deadline"
            dropped.add(it)
            continue
        }
        it.day = day
        used[day] = (used[day] ?: 0) + it.minutes
        items.add(it)
    }

    // 5. revision items (spaced by the forgetting curve)
    if (useForgetting && useRevision) {
        val P = model.params
        val events = ArrayList<Pair<Int, String>>()
        val doneDay = HashMap<String, Int>()
        for (it in items) if (it.type == "learn") doneDay[it.skillId!!] = it.day!!
        for (s in closure) {
            var sStab: Double
            var t: Double
            var p: Double
            val dd = doneDay[s]
            if (dd != null) {
                sStab = max(model.state(s).S, P.s0Days) * ASSUMED_GROWTH
                t = dd.toDouble(); p = max(tgt, cur.getValue(s))
            } else if (cur.getValue(s) > P.reviseBelow) {
                sStab = model.state(s).S
                t = now; p = cur.getValue(s)
            } else continue
            while (true) {
                val dt = if (p > P.reviseBelow) sStab * ln(p / P.reviseBelow) else 0.0
                t += max(1.0, dt)
                if (t > profile.deadlineDay) break
                events.add(ceil(t).toInt() to s)
                sStab *= ASSUMED_GROWTH
                p = max(tgt, p)
            }
        }
        events.sortWith(compareBy<Pair<Int, String>> { it.first }.thenBy { it.second })
        for ((d0, s) in events) {
            var d = nextStudyDay(max(d0, start))
            while ((used[d] ?: 0) + REVISE_MIN > capDay) {
                d = nextStudyDay(d + 1)
                if (d > profile.deadlineDay) break
            }
            if (d > profile.deadlineDay) continue
            used[d] = (used[d] ?: 0) + REVISE_MIN
            items.add(PlanItem("revise", skillId = s, day = d, minutes = REVISE_MIN, reason = "effective mastery will fall below 0.7"))
        }
    }

    items.sortWith(compareBy<PlanItem> { it.day!! }.thenBy { typeRank(it.type) })
    val nLearn = items.filter { it.type == "learn" }.map { it.skillId }.toSet().size
    val reason = "%s%d skills to learn, %d repair, %d revisions, %d dropped; target mastery %.1f, %.1f h/week to day %d".format(
        reasonPrefix, nLearn, items.count { it.type == "repair" }, items.count { it.type == "revise" },
        dropped.size, tgt, profile.hoursPerWeek, profile.deadlineDay)
    return Plan(version, now, reason, items, dropped)
}

/** Fraction of the next k items of the old plan (by order) that are no longer among the next k items of the new plan. */
fun planStability(old: Plan?, new: Plan, k: Int = 10): Double {
    if (old == null || old.items.isEmpty()) return 0.0
    fun key(i: PlanItem) = Triple(i.type, i.skillId, i.misconceptionId)
    val a = old.items.take(k).map(::key)
    val b = new.items.take(k).map(::key).toSet()
    return a.count { it !in b }.toDouble() / a.size
}
