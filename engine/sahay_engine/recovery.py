"""Missed-day recovery with knapsack replanning (PRD 9.6, feature I6).

The remaining hours are a budget. Item value = importance x mastery gap, cost = hours. Prerequisite closure is
enforced: an item is kept only if every unmastered prerequisite item is kept. Dropped items are listed with a reason.
Method: exact 0/1 knapsack on minute weights, then closure repair and refill, repeated until stable.
(The closure-constrained problem is NP-hard in general; this is a heuristic, tested against brute force.)
"""
from .planner import Plan, PlanItem


def _knapsack(items, budget):
    """items: list of (key, value, cost_minutes). Returns set of chosen keys. Exact DP on integer minutes."""
    n = len(items)
    best = [[0.0] * (budget + 1) for _ in range(n + 1)]
    for i, (_, v, c) in enumerate(items, 1):
        row, prev = best[i], best[i - 1]
        for w in range(budget + 1):
            row[w] = prev[w]
            if c <= w and prev[w - c] + v > row[w]:
                row[w] = prev[w - c] + v
    chosen, w = set(), budget
    for i in range(n, 0, -1):
        if best[i][w] != best[i - 1][w]:
            chosen.add(items[i - 1][0])
            w -= items[i - 1][2]
    return chosen


def recover(content, model, profile, now, targets, old_plan, budget_minutes, version=2):
    """Re-pack the learn work of old_plan (not yet done) into budget_minutes while keeping prerequisite closure."""
    g = content.graph
    tgt = profile.target_mastery
    skills = {}
    for it in old_plan.items:
        if it.type == "learn" and it.status in ("planned", "skipped"):
            skills[it.skill_id] = skills.get(it.skill_id, 0) + it.minutes
    unmastered = set(skills)
    value = {}
    for s, m in skills.items():
        gap = max(0.0, tgt - model.p_eff(s, now))
        value[s] = profile.weights.get(s, 1.0) * gap * (1 + len([d for d in g.dependents(s) if d in unmastered]))
    pool = set(skills)
    chosen = set()
    for _ in range(len(skills) + 1):
        cand = [(s, value[s], skills[s]) for s in sorted(pool)]
        chosen = _knapsack(cand, int(budget_minutes))
        # closure repair: drop chosen items with an unmastered prerequisite that was not chosen
        changed = True
        while changed:
            changed = False
            for s in sorted(chosen):
                if any(p in unmastered and p not in chosen for p in g.prereq_closure(s)):
                    chosen.discard(s)
                    pool.discard(s)
                    changed = True
        # refill: add skills whose prerequisites are all chosen (or mastered) if they still fit
        used = sum(skills[s] for s in chosen)
        for s in sorted(pool - chosen, key=lambda x: -value[x] / skills[x]):
            if used + skills[s] <= budget_minutes and all((p not in unmastered) or (p in chosen) for p in g.prereq_closure(s)):
                chosen.add(s)
                used += skills[s]
        if all(all((p not in unmastered) or (p in chosen) for p in g.prereq_closure(s)) for s in chosen):
            break
    keep = [s for s in g.topo_order(chosen)]
    items, dropped = [], []
    day = None
    for it in old_plan.items:
        if it.type == "learn" and it.status in ("planned", "skipped"):
            if it.skill_id in chosen:
                items.append(it)
            else:
                d = PlanItem("learn", skill_id=it.skill_id, minutes=it.minutes, day=it.day, status="dropped")
                blocked = [p for p in g.prereq_closure(it.skill_id) if p in unmastered and p not in chosen]
                d.reason = ("a prerequisite (%s) was dropped" % blocked[0]) if blocked else "lowest value per hour for the time left"
                dropped.append(d)
        elif it.type in ("repair",):
            items.append(it)
    # re-lay the kept items on study days from tomorrow in topological order, respecting daily minutes
    import math
    start = int(math.ceil(now)) + 1
    order = {s: i for i, s in enumerate(keep)}
    items.sort(key=lambda x: (order.get(x.skill_id, -1), 0 if x.type == "repair" else 1))
    used, d = {}, start
    out = []
    while not profile.is_study_day(d, start):
        d += 1
    for it in items:
        while used.get(d, 0) + it.minutes > profile.day_minutes:
            d += 1
            while not profile.is_study_day(d, start):
                d += 1
        if d > profile.deadline_day:
            it.status, it.reason = "dropped", "not enough days before the deadline"
            dropped.append(it)
            continue
        it.day = d
        used[d] = used.get(d, 0) + it.minutes
        out.append(it)
    reason = "recovery: %d of %d skills kept within %d minutes; %d dropped" % (len(chosen), len(skills), budget_minutes, len({x.skill_id for x in dropped}))
    return Plan(version=version, created_day=now, reason=reason, items=out, dropped=dropped)
