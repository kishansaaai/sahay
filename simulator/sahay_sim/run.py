"""One simulated run of one learner under one planner condition (PRD 12.1, 12.2)."""
import math
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "engine"))
from sahay_engine import (Content, LearnerModel, MisconceptionEngine, Profile, build_plan, Diagnostic,  # noqa: E402
                          CalibrationTracker, recover)
from sahay_engine.planner import PlanItem, Plan, plan_stability  # noqa: E402
from .learner import Learner, EXPECTED_MIN_PER_Q  # noqa: E402

HORIZON = 84                      # days (12 weeks)
HPW, STUDY_DAYS, TARGET = 6.0, 5, 0.8
CONDITIONS = ["full", "fixed_linear", "topic_only", "no_forgetting", "random_order", "no_replan", "no_diagnostic"]
_CONTENT = {}


def content(domain):
    if domain not in _CONTENT:
        _CONTENT[domain] = Content.load(domain)
    return _CONTENT[domain]


def eligible_targets(c):
    g = c.graph
    return [s for s in sorted(g.skills) if 10 <= len(g.closure([s])) <= 24]


def make_learner(domain, ltype, idx, seed0=20261110):
    c = content(domain)
    rng = random.Random("%s|%s|%d|%d" % (domain, ltype, idx, seed0))
    target = rng.choice(eligible_targets(c))
    return Learner(c, ltype, rng.randrange(1 << 30), [target])


def auc(pairs):
    pos = [p for p, y in pairs if y]
    neg = [p for p, y in pairs if not y]
    if not pos or not neg:
        return None
    ranked = sorted(pairs, key=lambda x: x[0])
    # average ranks for ties
    ranks, i = {}, 0
    while i < len(ranked):
        j = i
        while j + 1 < len(ranked) and ranked[j + 1][0] == ranked[i][0]:
            j += 1
        for k in range(i, j + 1):
            ranks[k] = (i + j) / 2.0 + 1
        i = j + 1
    sr = sum(ranks[k] for k, (p, y) in enumerate(ranked) if y)
    return (sr - len(pos) * (len(pos) + 1) / 2.0) / (len(pos) * len(neg))


def static_plan(c, order, targets_minutes_fn):
    items = []
    for s in order:
        items.append(PlanItem("learn", skill_id=s, minutes=int(round(targets_minutes_fn(s)))))
    return Plan(1, 0, "static", items)


def run(domain, ltype, idx, cond, horizon=HORIZON, recovery_gap=None, hpw=HPW):
    """recovery_gap = (start_day, length_days, mode) with mode 'replan' or 'recover'; weekly replans are then off."""
    c = content(domain)
    L = make_learner(domain, ltype, idx)
    rng = random.Random("run|%s|%s|%d|%s" % (domain, ltype, idx, cond))
    attend_rng = random.Random("attend|%s|%s|%d" % (domain, ltype, idx))   # identical attendance across conditions
    g = c.graph
    prof = Profile(hours_per_week=hpw, study_days=STUDY_DAYS, deadline_day=horizon, target_mastery=TARGET)
    cap = prof.day_minutes
    engine_on = cond in ("full", "topic_only", "no_forgetting", "no_replan", "no_diagnostic")
    model, mis, cal = LearnerModel(), MisconceptionEngine(), CalibrationTracker()
    preds = []
    overhead_min = 0.0
    kw = dict(use_misconceptions=cond != "topic_only", use_forgetting=cond != "no_forgetting")
    stab, version = [], 1

    def ask(skill, t, n=2, only_mid=None):
        qs = c.by_skill[skill]
        if only_mid:
            qs = [q for q in qs if any(o["misconception"] == only_mid for o in q["options"])] or qs
        for q in rng.sample(qs, min(n, len(qs))):
            ps = model.p_eff(skill, t) if cond != "no_forgetting" else model.state(skill).p
            gq = 1.0 / len(q["options"])
            pred = ps * (1 - model.params.slip) + (1 - ps) * gq
            idx_, conf, rt, ok = L.answer(q, t)
            preds.append((pred, ok))
            model.observe(skill, ok, conf, rt, 0, t, n_options=len(q["options"]))
            mis.observe(q, idx_, conf, t)
            cal.add(conf, ok)

    # ---- diagnostic -------------------------------------------------
    if engine_on and cond != "no_diagnostic":
        d = Diagnostic(c, model, mis, L.targets, now=0.0)
        while not d.done():
            q = d.next_question()
            idx_, conf, rt, ok = L.answer(q, 0.0)
            ps = model.state(q["skill"]).p
            gq = 1.0 / len(q["options"])
            preds.append((ps * 0.9 + (1 - ps) * gq, ok))
            d.record(q, idx_, conf, rt)
            overhead_min += rt / 60000.0

    # ---- initial plan -----------------------------------------------
    if cond == "fixed_linear":
        order = g.topo_order(L.closure)
        plan = static_plan(c, order, lambda s: g.est_hours(s) * 60)
    elif cond == "random_order":
        order = list(L.closure)
        random.Random("order|%s|%s|%d" % (domain, ltype, idx)).shuffle(order)
        plan = static_plan(c, order, lambda s: g.est_hours(s) * 60)
    else:
        plan = build_plan(c, model, mis, prof, 0, L.targets, version=1, **kw)
    first_violations = plan.prerequisite_violations(c)
    queue = [PlanItem(i.type, i.skill_id, i.misconception_id, i.day, i.minutes) for i in plan.items]
    adaptive = cond in ("full", "topic_only", "no_forgetting", "no_diagnostic")
    missed = 0
    hours_to_target, retention, reached = None, None, False
    t_reach = None

    gap_end = (recovery_gap[0] + recovery_gap[1]) if recovery_gap else None
    returned = False
    first_return = False
    for day in range(1, horizon + 1):
        if not prof.is_study_day(day, 1):
            continue
        if recovery_gap and recovery_gap[0] <= day < gap_end:
            missed += 1
            continue
        if not L.attends(attend_rng):
            missed += 1
            continue
        if recovery_gap:
            first_return = engine_on and day >= gap_end and not returned
            replan_now = first_return or (engine_on and day > gap_end and not queue)   # re-plan on demand when the queue runs dry
            returned = returned or day >= gap_end
        else:
            replan_now = adaptive and day > 1 and (missed >= 2 or (day - 1) % 7 == 0)
        if replan_now:
            version += 1
            old = plan
            if recovery_gap and recovery_gap[2] == "recover" and first_return:
                budget = int(cap * sum(1 for d in range(day, horizon + 1) if prof.is_study_day(d, 1)))
                plan = recover(c, model, prof, day - 1, L.targets, None, budget, mis=mis, version=version)
            else:
                plan = build_plan(c, model, mis, prof, day - 1, L.targets, version=version, **kw)
            stab.append(plan_stability(Plan(version, day, 'old', [PlanItem(i.type, i.skill_id, i.misconception_id, i.day, i.minutes) for i in queue]), plan))
            queue = [PlanItem(i.type, i.skill_id, i.misconception_id, i.day, i.minutes) for i in plan.items]
        missed = 0
        remaining = cap
        t = float(day)
        while queue and remaining > 0:
            it = queue[0]
            take = min(it.minutes, remaining)
            if it.type == "learn":
                L.study(it.skill_id, take, t)
            elif it.type == "revise":
                L.revise(it.skill_id, take, t)
            elif it.type == "repair":
                L.repair(it.skill_id, it.misconception_id, take, t)
            it.minutes -= take
            remaining -= take
            if engine_on and take >= 10:
                ask(it.skill_id, t, 2, it.misconception_id if it.type == "repair" else None)
            if it.minutes <= 0:
                queue.pop(0)
        if t_reach is None and L.mean_k(t) >= TARGET:
            t_reach, reached = t, True
            break
        if not queue and not adaptive:
            break

    end_t = float(t_reach if reached else horizon)
    hours = (L.minutes + overhead_min) / 60.0
    retention = sum(L.k[s] * math.exp(-30.0 / L.S[s]) * math.exp(-max(end_t - L.last[s], 0.0) / L.S[s]) for s in L.closure) / len(L.closure)
    resolved = (len(L.mis0 - L.mis) / len(L.mis0)) if L.mis0 else None
    return dict(domain=domain, type=ltype, idx=idx, cond=cond, hours=hours, reached=reached,
                end_day=end_t, mean_k_end=L.mean_k(end_t), retention30=retention, mis_resolved=resolved,
                prereq_violations=first_violations, auc=auc(preds) if engine_on else None,
                stability=(sum(stab) / len(stab)) if stab else None, n_mis=len(L.mis0))
