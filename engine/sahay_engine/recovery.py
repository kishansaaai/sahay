"""Missed-day recovery with knapsack replanning (PRD 9.6, feature I6).

The remaining study time is a budget. Item value = importance x mastery gap (PRD 9.6), item cost = minutes to learn it, and prerequisite closure is enforced: a skill is kept
only if every unmastered prerequisite is kept. The chosen skills are then sequenced, repaired and revised by the
ordinary planner (same state, same rules), and every dropped skill is listed with the reason.

Selection: exact 0/1 knapsack on minutes, then closure repair and refill, repeated until stable. The closure-constrained
problem is NP-hard in general; this is a heuristic, tested against brute force on a small graph (>= 90% of optimum).
`old_plan` is accepted for API compatibility and only used for its weights when `profile.weights` is empty.
"""
import math

from .misconceptions import MisconceptionEngine
from .planner import Plan, PlanItem, build_plan, learn_minutes


def _knapsack(items, budget):
    """items: list of (key, value, cost_minutes). Returns the set of chosen keys. Exact DP on integer minutes."""
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


def recover(content, model, profile, now, targets, old_plan, budget_minutes, mis=None, version=2):
    g = content.graph
    tgt = profile.target_mastery
    mis = mis or MisconceptionEngine()
    closure = g.closure(targets)
    gap = {s: tgt - model.p_eff(s, now) for s in closure}
    pool = {s for s in closure if gap[s] > 1e-9}
    cost = {s: learn_minutes(content, s, gap[s], tgt) for s in pool}
    value = {s: profile.weights.get(s, 1.0) * gap[s] for s in pool}          # PRD 9.6: importance x mastery gap
    avail = set(pool)
    chosen = set()
    for _ in range(len(pool) + 1):
        chosen = _knapsack([(s, value[s], cost[s]) for s in sorted(avail)], int(budget_minutes))
        changed = True
        while changed:                        # closure repair: drop chosen skills whose unmastered prerequisite is not chosen
            changed = False
            for s in sorted(chosen):
                if any(p in pool and p not in chosen for p in g.prereq_closure(s)):
                    chosen.discard(s)
                    avail.discard(s)
                    changed = True
        used = sum(cost[s] for s in chosen)   # refill with skills whose prerequisites are all kept
        for s in sorted(avail - chosen, key=lambda x: (-value[x] / cost[x], x)):
            if used + cost[s] <= budget_minutes and all((p not in pool) or (p in chosen) for p in g.prereq_closure(s)):
                chosen.add(s)
                used += cost[s]
        if all(all((p not in pool) or (p in chosen) for p in g.prereq_closure(s)) for s in chosen):
            break
    if chosen:
        plan = build_plan(content, model, mis, profile, now, sorted(chosen), version=version)
    else:
        plan = Plan(version, now, "", [])
    dropped = list(plan.dropped)
    for s in sorted(pool - chosen):
        blocked = sorted(p for p in g.prereq_closure(s) if p in pool and p not in chosen)
        reason = ("prerequisite %s was dropped" % blocked[0]) if blocked else "lowest value per hour for the time left"
        dropped.append(PlanItem("learn", skill_id=s, minutes=cost[s], status="dropped", reason=reason))
    plan.dropped = dropped
    plan.reason = "recovery: %d of %d skills kept within %d minutes; %d dropped" % (len(chosen), len(pool), budget_minutes, len(pool - chosen))
    return plan
