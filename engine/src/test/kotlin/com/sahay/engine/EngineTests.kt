package com.sahay.engine

import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Port of engine/tests/test_engine.py. Dependency-free runner so it compiles with plain kotlinc and runs in Gradle
 * (task `engineTests`). Reference numbers are hand-computed from PRD section 9. Run: java -cp ... com.sahay.engine.EngineTestsKt <content dir>
 */
private var failures = 0
private var passed = 0

private fun check(cond: Boolean, msg: String) { if (!cond) throw AssertionError(msg) }
private fun near(a: Double, b: Double, eps: Double, msg: String = "") = check(abs(a - b) <= eps, "$msg expected $b got $a")

private fun test(name: String, body: () -> Unit) {
    try { body(); passed++; println("PASS $name") } catch (e: Throwable) { failures++; println("FAIL $name: ${e.message}") }
}

private lateinit var C: Content

private fun qWithDistractor(skill: String = "dsa.stack"): Triple<Question, Int, Int> {
    val q = C.bySkill.getValue(skill)[0]
    return Triple(q, q.options.indexOfFirst { it.misconception != null }, q.answer)
}

private fun randomLearner(rng: Random, targets: List<String>): Triple<LearnerModel, MisconceptionEngine, Double> {
    val m = LearnerModel(); val mis = MisconceptionEngine()
    val now = rng.nextDouble() * 20
    for (s in C.graph.closure(targets)) if (rng.nextDouble() < 0.6) {
        val st = m.state(s)
        st.p = rng.nextDouble(); st.S = 1 + rng.nextDouble() * 11; st.lastSeen = now - rng.nextDouble() * 15
    }
    repeat(rng.nextInt(0, 4)) {
        val sk = C.graph.closure(targets).sorted().random(rng)
        val q = C.bySkill.getValue(sk).random(rng)
        val wr = q.options.indices.filter { q.options[it].misconception != null }
        repeat(2) { mis.observe(q, wr.random(rng), 90.0, now) }
    }
    return Triple(m, mis, now)
}

fun main(args: Array<String>) {
    val root = File(args.getOrElse(0) { "content" })
    C = Content.fromJson(File(root, "skillgraphs/dsa.json").readText(), File(root, "questions/dsa.json").readText())

    // ---- knowledge ----
    test("bkt correct matches reference") {
        val post = 0.2 * 0.9 / (0.2 * 0.9 + 0.8 * 0.25)
        near(bktUpdate(0.2, true, 0.10, 0.25, 0.15), post + (1 - post) * 0.15, 1e-12)
        near(bktUpdate(0.2, true, 0.10, 0.25, 0.15), 0.552631578947, 1e-9)
    }
    test("bkt wrong matches reference") {
        val post = 0.2 * 0.1 / (0.2 * 0.1 + 0.8 * 0.75)
        near(bktUpdate(0.2, false, 0.10, 0.25, 0.15), post + (1 - post) * 0.15, 1e-12)
        near(bktUpdate(0.2, false, 0.10, 0.25, 0.15), 0.177419354839, 1e-9)
    }
    test("hint weights scale update toward prior") {
        val full = bktUpdate(0.2, true, 0.1, 0.25, 0.15, 1.0)
        for (w in listOf(1.0, 0.7, 0.4, 0.0)) near(bktUpdate(0.2, true, 0.1, 0.25, 0.15, w), 0.2 + w * (full - 0.2), 1e-12)
        check(bktUpdate(0.2, true, 0.1, 0.25, 0.15, 0.0) == 0.2, "w=0 leaves the prior")
    }
    test("mastery stays in unit interval") {
        val rng = Random(1)
        repeat(300) {
            val m = LearnerModel(); var t = 0.0
            repeat(60) {
                t += rng.nextDouble() * 5
                m.observe("s", rng.nextDouble() < 0.5, rng.nextInt(0, 101).toDouble(), rng.nextInt(500, 90000).toDouble(), rng.nextInt(0, 4), t)
                check(m.state("s").p in 0.0..1.0, "p out of range")
                check(m.pEff("s", t + rng.nextDouble() * 100) in 0.0..1.0, "p_eff out of range")
            }
        }
    }
    test("guess comes from option count") {
        val a = LearnerModel(); val b = LearnerModel()
        a.observe("s", true, 50.0, 45000.0, 0, 0.0, nOptions = 4)
        b.observe("s", true, 50.0, 45000.0, 0, 0.0, nOptions = 2)
        check(a.state("s").p > b.state("s").p, "less guessable question gives more evidence")
    }
    test("retention and effective mastery") {
        near(retention(3.0, 3.0), exp(-1.0), 1e-12)
        val m = LearnerModel(); val st = m.state("s"); st.p = 0.9; st.S = 4.0; st.lastSeen = 0.0
        near(m.pEff("s", 4.0), 0.9 * exp(-1.0), 1e-12)
    }
    test("stability growth and failure") {
        val m = LearnerModel(); val st = m.state("s"); st.S = 2.0
        m.observe("s", true, 100.0, 1000.0, 0, 0.0); near(st.S, 5.0, 1e-9, "fast confident")
        m.observe("s", true, 0.0, 1e9, 0, 1.0); near(st.S, 7.5, 1e-3, "slow unconfident")
        st.S = 1.2; m.observe("s", false, 50.0, 5000.0, 0, 2.0); check(st.S == 1.0, "floor of 1 day")
        st.S = 10.0; m.observe("s", false, 50.0, 5000.0, 0, 3.0); check(st.S == 5.0, "halved")
    }
    test("revision threshold is 0.7") {
        val m = LearnerModel(); val st = m.state("s"); st.p = 0.9; st.S = 10.0; st.lastSeen = 0.0
        val t = 10.0 * ln(0.9 / 0.7)
        check(!m.needsRevision("s", t - 0.5), "before"); check(m.needsRevision("s", t + 0.5), "after")
    }

    // ---- misconceptions ----
    test("confident wrong counts double a guess") {
        val (q, wrong, _) = qWithDistractor(); val mid = q.options[wrong].misconception!!
        val a = MisconceptionEngine(); val b = MisconceptionEngine()
        a.observe(q, wrong, 90.0, 0.0); b.observe(q, wrong, 10.0, 0.0)
        near(a.state.getValue(mid).evidence / b.state.getValue(mid).evidence, 2.0, 1e-9)
    }
    test("two confident hits create repair; guesses do not") {
        val (q, wrong, _) = qWithDistractor(); val mid = q.options[wrong].misconception!!
        val e = MisconceptionEngine()
        e.observe(q, wrong, 90.0, 0.0); check(e.repairs().isEmpty(), "one hit")
        e.observe(q, wrong, 85.0, 1.0); check(e.repairs() == listOf(mid), "two hits")
        val g = MisconceptionEngine()
        for (d in 0 until 5) g.observe(q, wrong, 20.0, d.toDouble())
        check(g.repairs().isEmpty(), "guesses")
    }
    test("resolution needs three correct over two days") {
        val (q, wrong, right) = qWithDistractor(); val mid = q.options[wrong].misconception!!
        val e = MisconceptionEngine()
        e.observe(q, wrong, 90.0, 0.0); e.observe(q, wrong, 90.0, 0.1)
        for (d in listOf(1.0, 1.5, 1.9)) e.observe(q, right, 80.0, d)
        check(!e.isResolved(mid), "span under two days")
        e.observe(q, right, 80.0, 3.0); check(!e.isResolved(mid), "1.5 day span")
        e.observe(q, right, 80.0, 4.0); check(e.isResolved(mid), "2.1 day span")
        check(e.repairs().isEmpty(), "no repair once resolved")
    }
    test("relapse reopens") {
        val (q, wrong, right) = qWithDistractor(); val mid = q.options[wrong].misconception!!
        val e = MisconceptionEngine()
        e.observe(q, wrong, 90.0, 0.0); e.observe(q, wrong, 90.0, 0.1)
        for (d in listOf(1.0, 2.0, 3.5)) e.observe(q, right, 80.0, d)
        check(e.isResolved(mid), "resolved"); e.observe(q, wrong, 90.0, 5.0); check(!e.isResolved(mid), "reopened")
    }

    // ---- calibration ----
    test("bias brier labels") {
        val t = CalibrationTracker(); repeat(10) { t.add(90.0, false) }
        near(t.bias(), 0.9, 1e-9); near(t.brier(), 0.81, 1e-9)
        check(t.label() == OVERCONFIDENT, "over"); check(t.policy().askJustification, "justification")
        val u = CalibrationTracker(); repeat(10) { u.add(20.0, true) }
        near(u.bias(), -0.8, 1e-9); check(u.label() == UNDERCONFIDENT, "under")
        check(u.policy().ladder.levels == listOf(0, 2, 3), "short ladder")
    }
    test("calibration threshold is strict") {
        val t = CalibrationTracker()
        for (i in 0 until 20) t.add(65.0, i < 10)
        check(t.label() == CALIBRATED, "bias +0.15 exactly is not above")
        t.add(100.0, false); check(t.label() == OVERCONFIDENT, "now over")
    }
    test("hint ladder steps and mastery weights") {
        val h = HintLadder(); val seq = ArrayList<Int>(); var cur: Int? = null
        while (true) { cur = h.nextLevel(cur) ?: break; seq.add(cur) }
        check(seq == listOf(0, 1, 2, 3), "ladder $seq")
        val a = LearnerModel(); val b = LearnerModel(); val c = LearnerModel()
        a.observe("s", true, 50.0, 45000.0, 0, 0.0); b.observe("s", true, 50.0, 45000.0, 2, 0.0); c.observe("s", true, 50.0, 45000.0, 3, 0.0)
        check(b.state("s").p < a.state("s").p, "hint lowers credit"); check(c.state("s").p == 0.2, "solution gives none")
    }

    // ---- diagnostic ----
    test("diagnostic asks 10 to 15 questions") {
        for (k in listOf(0.0, 0.3, 0.7, 1.0)) for (seed in 0 until 5) {
            val rng = Random(seed); val m = LearnerModel(); val mis = MisconceptionEngine()
            val d = Diagnostic(C, m, mis, listOf("dsa.dp_2d")); var n = 0
            while (!d.done()) {
                val q = d.nextQuestion()!!
                val idx = if (rng.nextDouble() < k) q.answer else q.options.indices.filter { q.options[it].misconception != null }.random(rng)
                d.record(q, idx, 70.0, 40000.0); n++
            }
            check(n in 10..15, "k=$k seed=$seed n=$n")
        }
    }

    // ---- planner ----
    test("planner valid across 1000 learners and fast") {
        val rng = Random(7); val skills = C.graph.skills.keys.sorted(); var worst = 0L
        for (i in 0 until 1000) {
            val targets = skills.shuffled(rng).take(rng.nextInt(1, 4))
            val (m, mis, now) = randomLearner(rng, targets)
            val prof = Profile(listOf(3.0, 6.0, 10.0).random(rng), listOf(3, 5, 6).random(rng), now.toInt() + listOf(14, 35, 70).random(rng))
            val t0 = System.nanoTime()
            val plan = buildPlan(C, m, mis, prof, now, targets)
            worst = maxOf(worst, System.nanoTime() - t0)
            val mastered = C.graph.closure(targets).filter { m.pEff(it, now) >= prof.targetMastery }.toSet()
            val droppedSkills = plan.dropped.map { it.skillId }.toSet()
            check(plan.prerequisiteViolations(C, mastered) == 0, "violations learner $i")
            val planned = plan.items.filter { it.type == "learn" }.map { it.skillId!! }.toSet()
            for (s in planned) for (p in C.graph.prereqs(s)) check(p in planned || p in mastered || p in droppedSkills, "unplanned prerequisite $i $s $p")
            check(plan.reason.isNotEmpty(), "reason")
            val perDay = HashMap<Int, Int>()
            for (it in plan.items) { check(it.day!! <= prof.deadlineDay, "deadline"); perDay[it.day!!] = (perDay[it.day!!] ?: 0) + it.minutes }
            check(perDay.values.all { it <= prof.dayMinutes }, "day minutes")
        }
        check(worst < 1_000_000_000L, "slowest plan ${worst / 1e6} ms")
    }
    test("weekly hours not exceeded") {
        val prof = Profile(hoursPerWeek = 4.0, deadlineDay = 120)
        val plan = buildPlan(C, LearnerModel(), MisconceptionEngine(), prof, 0.0, listOf("dsa.shortest"))
        for ((_, its) in plan.weeks(1)) check(its.sumOf { it.minutes } <= prof.hoursPerWeek * 60 + 1, "week over budget")
    }
    test("repair item placed before its skill") {
        val m = LearnerModel(); val mis = MisconceptionEngine()
        val (q, wrong, _) = qWithDistractor("dsa.arrays")
        mis.observe(q, wrong, 90.0, 0.0); mis.observe(q, wrong, 90.0, 0.5)
        val plan = buildPlan(C, m, mis, Profile(deadlineDay = 100), 1.0, listOf("dsa.binary_search"))
        val r = plan.items.indexOfFirst { it.type == "repair" && it.skillId == "dsa.arrays" }
        val l = plan.items.indexOfFirst { it.type == "learn" && it.skillId == "dsa.arrays" }
        check(r >= 0 && l >= 0 && r < l, "repair $r learn $l")
    }
    test("revision scheduled and disabled without forgetting") {
        val full = buildPlan(C, LearnerModel(), MisconceptionEngine(), Profile(deadlineDay = 60), 0.0, listOf("dsa.arrays"))
        val nof = buildPlan(C, LearnerModel(), MisconceptionEngine(), Profile(deadlineDay = 60), 0.0, listOf("dsa.arrays"), useForgetting = false)
        check(full.items.any { it.type == "revise" }, "has revisions"); check(nof.items.none { it.type == "revise" }, "no revisions")
    }
    test("mastered skills are not planned") {
        val m = LearnerModel()
        for (s in C.graph.prereqClosure("dsa.dp_1d") + "dsa.dp_intro") { val st = m.state(s); st.p = 0.99; st.S = 50.0; st.lastSeen = 0.0 }
        val plan = buildPlan(C, m, MisconceptionEngine(), Profile(deadlineDay = 80), 0.0, listOf("dsa.dp_1d"))
        check(plan.learnOrder() == listOf("dsa.dp_1d"), "order ${plan.learnOrder()}")
    }
    test("deadline drops with reason; stability metric") {
        val plan = buildPlan(C, LearnerModel(), MisconceptionEngine(), Profile(hoursPerWeek = 2.0, deadlineDay = 7), 0.0, listOf("dsa.dp_2d"))
        check(plan.dropped.isNotEmpty() && plan.dropped.all { it.reason.isNotEmpty() }, "dropped with reasons")
        check(planStability(plan, plan) == 0.0, "same plan is stable")
    }

    // ---- recovery ----
    val rm = LearnerModel(); val rmis = MisconceptionEngine(); val rprof = Profile(hoursPerWeek = 8.0, deadlineDay = 60)
    val rtargets = listOf("dsa.dp_2d", "dsa.topo")
    val old = buildPlan(C, rm, rmis, rprof, 0.0, rtargets)
    val totalLearn = old.items.filter { it.type == "learn" }.sumOf { it.minutes }
    test("recovery respects closure, budget, reasons") {
        for (frac in listOf(0.2, 0.4, 0.6, 0.8)) {
            val budget = (totalLearn * frac).toInt()
            val rec = recover(C, rm, rprof, 0.0, rtargets, old, budget, mis = rmis)
            val kept = rec.items.filter { it.type == "learn" }.map { it.skillId!! }.toSet()
            check(rec.items.filter { it.type == "learn" }.sumOf { it.minutes } <= budget, "budget $frac")
            for (s in kept) for (p in C.graph.prereqClosure(s)) check(p in kept || rm.pEff(p, 0.0) >= 0.8, "closure $frac $s $p")
            check(rec.dropped.all { it.reason.isNotEmpty() } && rec.reason.isNotEmpty(), "reasons")
        }
    }
    test("full budget keeps everything") {
        check(recover(C, rm, rprof, 0.0, rtargets, old, totalLearn + 600).dropped.isEmpty(), "nothing dropped")
    }
    test("recovery within 90 percent of brute force optimum") {
        val m = LearnerModel(); val t = listOf("dsa.binary_search")
        val o = buildPlan(C, m, MisconceptionEngine(), rprof, 0.0, t)
        val minutes = HashMap<String, Int>()
        for (i in o.items) if (i.type == "learn") minutes[i.skillId!!] = (minutes[i.skillId] ?: 0) + i.minutes
        val budget = (minutes.values.sum() * 0.6).toInt(); val ids = minutes.keys.toList()
        fun value(s: String) = maxOf(0.0, 0.8 - m.pEff(s, 0.0))
        var best = 0.0
        for (mask in 0 until (1 shl ids.size)) {
            val ss = ids.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.toSet()
            if (ss.sumOf { minutes.getValue(it) } > budget) continue
            if (ss.any { s -> C.graph.prereqClosure(s).any { it in minutes && it !in ss } }) continue
            best = maxOf(best, ss.sumOf { value(it) })
        }
        val rec = recover(C, m, rprof, 0.0, t, o, budget)
        val got = rec.items.filter { it.type == "learn" }.map { it.skillId!! }.toSet().sumOf { value(it) }
        check(got >= 0.9 * best - 1e-9, "got $got best $best")
    }

    // ---- pacing ----
    test("fatigue triggers on scripted pattern and never blocks") {
        val f = FatigueMonitor(); repeat(12) { f.add(30000.0, true) }
        check(f.suggestion().action == NONE, "steady")
        repeat(6) { f.add(90000.0, false) }
        val s = f.suggestion()
        check(s.action != NONE && s.dismissible && s.fatigue >= 0.6, "fatigued ${s.fatigue}")
    }
    test("steady learner not flagged") {
        val f = FatigueMonitor(); val rng = Random(3)
        repeat(40) { f.add(25000 + rng.nextDouble() * 10000, rng.nextDouble() < 0.8) }
        check(f.suggestion().action == NONE, "flagged ${f.index()}")
    }

    // ---- storage / snapshot (B04) ----
    test("snapshot round-trips through JSON") {
        val prof = LearnerProfileRow("Learn DSA for placements", "dsa", "beginner", 6.0, 5, 70, 0.8, listOf("cricket"), "en", 0.0)
        val plan = buildPlan(C, LearnerModel(), MisconceptionEngine(), prof.toPlannerProfile(), 0.0, listOf("dsa.arrays"))
        val q = C.bySkill.getValue("dsa.arrays")[0]
        val wrong = q.options.indexOfFirst { it.misconception != null }
        val snap = Snapshot(
            profile = prof, targets = listOf("dsa.arrays"),
            attempts = listOf(AttemptRow(1, q.id, q.skill, wrong, false, 90.0, 40000.0, 0, 1.0)),
            plan = plan, planChangeNote = "plan built", language = "en",
        )
        val back = Snapshot.fromJson(snap.toJson())
        check(back.toJson() == snap.toJson(), "round trip differs")
        check(back.attempts.single().questionId == q.id, "attempt kept")
        check(back.plan!!.items.size == plan.items.size, "plan items kept")
    }
    test("replay reproduces live state from attempts") {
        val targets = listOf("dsa.dp_1d")
        val m = LearnerModel(); val mis = MisconceptionEngine()
        val d = Diagnostic(C, m, mis, targets)
        val rows = ArrayList<AttemptRow>(); var seq = 1
        val rng = Random(11)
        while (!d.done()) {
            val q = d.nextQuestion()!!
            val correct = rng.nextDouble() < 0.6
            val idx = if (correct) q.answer else q.options.indices.filter { q.options[it].misconception != null }.random(rng)
            val conf = rng.nextInt(10, 95).toDouble(); val ms = rng.nextInt(8000, 60000).toDouble()
            d.record(q, idx, conf, ms)
            rows.add(AttemptRow(seq++, q.id, q.skill, idx, correct, conf, ms, 0, 0.0))
        }
        val rep = replay(C, targets, rows)
        for (s in C.graph.closure(targets)) near(rep.model.state(s).p, m.state(s).p, 1e-12, "p for $s")
        check(rep.calibration.n == rows.size, "calibration rows")
        check(rep.model.skills.isNotEmpty(), "state rebuilt")
    }
    test("snapshot import rejects malformed and unknown schema") {
        for (t in listOf("{}", "{\"schema\":\"nope\"}", "not json")) {
            val threw = try { Snapshot.fromJson(t); false } catch (e: IllegalArgumentException) { true } catch (e: Exception) { false }
            check(threw, "accepted $t")
        }
    }
    test("replay skips unknown questions without aborting") {
        val rep = replay(C, listOf("dsa.arrays"), listOf(AttemptRow(1, "missing.question", "dsa.arrays", 0, true, 50.0, 1000.0, 0, 0.0)))
        check(rep.model.state("dsa.arrays").p == LearnerModel().state("dsa.arrays").p, "unknown attempt must not change state")
        check(validateAttempts(C, listOf(AttemptRow(1, "missing.question", "dsa.arrays", 0, true, 50.0, 1000.0, 0, 0.0))).size == 1, "validator reports it")
    }

    // ---- forecast to goal (B10) ----
    test("forecast needs two study days") {
        val q = C.bySkill.getValue("dsa.arrays")[0]
        val rows = listOf(AttemptRow(1, q.id, q.skill, q.answer, true, 90.0, 9000.0, 0, 0.0))
        val f = forecastToTarget(C, listOf("dsa.arrays"), rows, now = 0.5, targetMastery = 0.99)
        check(f.basis == ForecastBasis.NOT_ENOUGH_DAYS, "basis ${f.basis}")
        check(f.daysToTarget == null, "no estimate without two days")
        check(f.currentMastery > 0.0, "current mastery ${f.currentMastery}")
        check(f.trajectory.size == 1, "one observed day")
    }
    test("forecast finds an upward trend") {
        val qs = C.bySkill.getValue("dsa.arrays")
        val rows = ArrayList<AttemptRow>(); var seq = 1
        for (day in 0..2) for (i in 0..1) {
            val q = qs[(day * 2 + i) % qs.size]
            rows.add(AttemptRow(seq++, q.id, q.skill, q.answer, true, 90.0, 9000.0, 0, day.toDouble()))
        }
        val f = forecastToTarget(C, listOf("dsa.arrays"), rows, now = 2.5, targetMastery = 0.95)
        check(f.trajectory.size == 3, "three days")
        check(f.gainPerDay > 0.0, "gain ${f.gainPerDay}")
        check(f.basis == ForecastBasis.ESTIMATE || f.basis == ForecastBasis.TOO_FAR, "basis ${f.basis}")
    }
    test("forecast reports a reached target without inventing days") {
        val f = forecastToTarget(C, listOf("dsa.arrays"), emptyList(), now = 0.0, targetMastery = 0.0)
        check(f.basis == ForecastBasis.TARGET_REACHED, "basis ${f.basis}")
        check(f.daysToTarget == 0, "zero days")
    }

    // ---- share code and teacher heatmap (B17) ----
    fun sampleSnapshot(n: Int = 12, reasonLen: Int = 80): Snapshot {
        val skills = C.graph.skills.keys.sorted()
        val items = (0 until n).map { i ->
            val reason = ("Because ${skills[i % skills.size]} unlocks the next step. ")
                .repeat((reasonLen / 44).coerceAtLeast(1)).take(reasonLen)
            PlanItem(
                type = listOf("learn", "practice", "revise")[i % 3],
                skillId = skills[i % skills.size],
                day = i,
                minutes = 20 + (i % 4) * 5,
                status = if (i == 0) "done" else "planned",
                reason = reason,
            )
        }
        val q = C.bySkill.getValue("dsa.arrays")[0]
        return Snapshot(
            profile = LearnerProfileRow(
                goal = "become a backend engineer", domain = "dsa", level = "beginner",
                hoursPerWeek = 8.0, studyDays = 5, deadlineDay = 30, targetMastery = 0.9,
                interests = listOf("cricket"), language = "en", createdAt = 20_000.0,
            ),
            targets = listOf("dsa.topo", "dsa.dp_1d"),
            attempts = listOf(AttemptRow(1, q.id, q.skill, q.answer, true, 80.0, 5_000.0, 0, 20_000.5)),
            plan = Plan(1, 20_000.0, "fits the deadline", items.toMutableList(), emptyList()),
            planChangeNote = "one skill moved",
            language = "en",
        )
    }

    test("share code round-trips a snapshot without loss") {
        val snap = sampleSnapshot()
        val back = ShareCode.decode(ShareCode.encode(snap))
        check(back.toJson() == snap.toJson(), "snapshot JSON differs after the round trip")
    }
    test("share code fits a typical plan in one QR symbol") {
        val code = ShareCode.encode(sampleSnapshot(n = 14, reasonLen = 80))
        check(ShareCode.qrFits(code), "code is ${code.toByteArray().size} bytes, above the QR ceiling")
    }
    test("share code rejects damaged or foreign text") {
        for (text in listOf("", "hello", ShareCode.PREFIX + "!!!", ShareCode.PREFIX + "AAAA")) {
            var rejected = false
            try { ShareCode.decode(text) } catch (e: ShareCode.InvalidShareCode) { rejected = true }
            check(rejected, "accepted '$text'")
        }
    }
    test("base64url matches the RFC 4648 vector") {
        check(Base64Url.encode("hello".toByteArray()) == "aGVsbG8", "encode mismatch")
        check(Base64Url.decode("aGVsbG8").toString(Charsets.UTF_8) == "hello", "decode mismatch")
    }
    test("heatmap merges students without inventing mastery") {
        val a = sampleSnapshot()
        val q = C.bySkill.getValue("dsa.arrays")[0]
        val b = a.copy(attempts = listOf(AttemptRow(1, q.id, q.skill, q.answer, false, 50.0, 9_000.0, 2, 20_001.0)))
        val rows = heatmap(C, listOf(a, b))
        check(rows.isNotEmpty(), "no skills in the heatmap")
        check(rows.all { it.students in 1..2 }, "student counts outside 1..2")
        check(rows.all { it.meanMastery in 0.0..1.0 }, "mastery outside 0..1")
        check(rows.map { it.skillId }.toSet().size == rows.size, "duplicate skill rows")
    }

    val (lp, lf) = llmTests(args[0]); passed += lp; failures += lf

    println("\n$passed passed, $failures failed")
    if (failures > 0) kotlin.system.exitProcess(1)
}
