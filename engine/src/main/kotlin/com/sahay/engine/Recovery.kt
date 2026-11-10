package com.sahay.engine

/**
 * Missed-day recovery with knapsack replanning (PRD 9.6, feature I6). Port of recovery.py.
 * Item value = importance x mastery gap, cost = minutes to learn, prerequisite closure enforced. Chosen skills are sequenced
 * by the ordinary planner; every dropped skill is listed with a reason.
 */
private fun knapsack(items: List<Triple<String, Double, Int>>, budget: Int): Set<String> {
    val n = items.size
    val best = Array(n + 1) { DoubleArray(budget + 1) }
    for (i in 1..n) {
        val (_, v, c) = items[i - 1]
        for (w in 0..budget) {
            best[i][w] = best[i - 1][w]
            if (c <= w && best[i - 1][w - c] + v > best[i][w]) best[i][w] = best[i - 1][w - c] + v
        }
    }
    val chosen = HashSet<String>()
    var w = budget
    for (i in n downTo 1) {
        if (best[i][w] != best[i - 1][w]) { chosen.add(items[i - 1].first); w -= items[i - 1].third }
    }
    return chosen
}

fun recover(
    content: Content, model: LearnerModel, profile: Profile, now: Double, targets: Collection<String>,
    oldPlan: Plan?, budgetMinutes: Int, mis: MisconceptionEngine? = null, version: Int = 2,
): Plan {
    val g = content.graph
    val tgt = profile.targetMastery
    val m = mis ?: MisconceptionEngine()
    val closure = g.closure(targets)
    val gap = closure.associateWith { tgt - model.pEff(it, now) }
    val pool = closure.filter { gap.getValue(it) > 1e-9 }.toSet()
    val cost = pool.associateWith { learnMinutes(content, it, gap.getValue(it), tgt) }
    val value = pool.associateWith { (profile.weights[it] ?: 1.0) * gap.getValue(it) }
    val avail = pool.toMutableSet()
    var chosen = HashSet<String>()
    for (iter in 0..pool.size) {
        chosen = HashSet(knapsack(avail.sorted().map { Triple(it, value.getValue(it), cost.getValue(it)) }, budgetMinutes))
        var changed = true
        while (changed) {      // closure repair: drop chosen skills whose unmastered prerequisite is not chosen
            changed = false
            for (s in chosen.sorted()) {
                if (g.prereqClosure(s).any { it in pool && it !in chosen }) {
                    chosen.remove(s); avail.remove(s); changed = true
                }
            }
        }
        var used = chosen.sumOf { cost.getValue(it) }     // refill with skills whose prerequisites are all kept
        for (s in (avail - chosen).sortedWith(compareBy<String> { -value.getValue(it) / cost.getValue(it) }.thenBy { it })) {
            if (used + cost.getValue(s) <= budgetMinutes && g.prereqClosure(s).all { it !in pool || it in chosen }) {
                chosen.add(s); used += cost.getValue(s)
            }
        }
        if (chosen.all { s -> g.prereqClosure(s).all { it !in pool || it in chosen } }) break
    }
    val plan = if (chosen.isNotEmpty()) buildPlan(content, model, m, profile, now, chosen.sorted(), version = version)
    else Plan(version, now, "", ArrayList())
    val dropped = ArrayList(plan.dropped)
    for (s in (pool - chosen).sorted()) {
        val blocked = g.prereqClosure(s).filter { it in pool && it !in chosen }.sorted()
        val reason = if (blocked.isNotEmpty()) "prerequisite ${blocked.first()} was dropped" else "lowest value per hour for the time left"
        dropped.add(PlanItem("learn", skillId = s, minutes = cost.getValue(s), status = "dropped", reason = reason))
    }
    plan.dropped = dropped
    plan.reason = "recovery: %d of %d skills kept within %d minutes; %d dropped".format(chosen.size, pool.size, budgetMinutes, (pool - chosen).size)
    return plan
}
