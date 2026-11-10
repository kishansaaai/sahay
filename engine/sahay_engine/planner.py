"""Planner and re-planner (PRD 9.5 and 9.6, feature C5).

1. target set = goal skills plus all prerequisites
2. need = (target mastery - p_eff) * goal weight / estimated hours
3. topological order; among ready skills pick highest need
4. fill days within the declared hours; repair items go directly before the skills they unblock
5. revision items for skills whose p_eff will cross 0.7 before the deadline
6. interleave: never more than two sessions of the same skill in a row; every day within the stated minutes
Returns a Plan with a reason string. The LLM only narrates a plan, it never changes the order.
Day numbers are integers (absolute). The first study day of a plan is day ceil(now) + 1.
"""
import math
from dataclasses import dataclass, field

SESSION_MIN = 30          # ASSUMPTION: maximum length of one study session chunk
REPAIR_MIN = 15
REVISE_MIN = 10
MIN_LEARN_MIN = 20
ASSUMED_GROWTH = 2.0      # stability multiplier assumed for a planned learn or revise


@dataclass
class Profile:
    hours_per_week: float = 6.0
    study_days: int = 5                 # study days per 7-day block
    deadline_day: int = 70
    target_mastery: float = 0.8
    weights: dict = field(default_factory=dict)   # goal weight per skill, default 1.0

    @property
    def day_minutes(self):
        return int(round(self.hours_per_week * 60.0 / self.study_days))

    def is_study_day(self, day, start):
        return ((day - start) % 7) < self.study_days


@dataclass
class PlanItem:
    type: str                 # learn | practice | revise | repair
    skill_id: str = None
    misconception_id: str = None
    day: int = None
    minutes: int = 0
    status: str = "planned"   # planned | done | skipped | dropped
    reason: str = ""


@dataclass
class Plan:
    version: int
    created_day: float
    reason: str
    items: list
    dropped: list = field(default_factory=list)

    def weeks(self, start):
        out = {}
        for it in self.items:
            out.setdefault((it.day - start) // 7, []).append(it)
        return out

    def learn_order(self):
        seen, order = set(), []
        for it in self.items:
            if it.type == "learn" and it.skill_id not in seen:
                seen.add(it.skill_id)
                order.append(it.skill_id)
        return order

    def prerequisite_violations(self, content, mastered=()):
        """Count planned learn skills whose first session is not after the last session of every planned DIRECT prerequisite.
        A prerequisite that is not in the plan is skipped here (it was judged mastered or dropped at the tail); the
        planner tests separately that every unplanned prerequisite really is mastered or dropped."""
        first, last = {}, {}
        for i, it in enumerate(self.items):
            if it.type == "learn":
                first.setdefault(it.skill_id, i)
                last[it.skill_id] = i
        bad = 0
        for s, f in first.items():
            for p in content.graph.prereqs(s):
                if p in mastered:
                    continue
                if p in last and last[p] > f:
                    bad += 1
                    break
        return bad


def learn_minutes(content, skill, gap, target):
    """Minutes the planner allots to learn a skill whose mastery gap is `gap` (shared with recovery)."""
    return max(MIN_LEARN_MIN, int(round(content.graph.est_hours(skill) * 60 * min(1.0, gap / target))))


def _chunks(total, cap=SESSION_MIN):
    out = []
    while total > 0:
        c = min(cap, total)
        out.append(c)
        total -= c
    return out


def build_plan(content, model, mis, profile, now, targets, version=1,
               use_forgetting=True, use_misconceptions=True, use_revision=True, reason_prefix=""):
    g = content.graph
    tgt = profile.target_mastery
    closure = g.closure(targets)
    cur = {}
    for s in closure:
        cur[s] = model.p_eff(s, now) if use_forgetting else model.state(s).p
    todo = {s for s in closure if tgt - cur[s] > 1e-9}

    def need(s):
        return (tgt - cur[s]) * profile.weights.get(s, 1.0) / g.est_hours(s)

    # 1-3. topological order with highest-need-first among ready skills
    order, placed = [], set()
    remaining = set(todo)
    while remaining:
        ready = [s for s in remaining if all((p not in todo) or (p in placed) for p in g.prereqs(s))]
        s = max(ready, key=lambda x: (need(x), x))
        remaining.discard(s)
        placed.add(s)
        order.append(s)

    seq = []
    for s in order:
        minutes = learn_minutes(content, s, tgt - cur[s], tgt)
        for c in _chunks(minutes):
            seq.append(PlanItem("learn", skill_id=s, minutes=c, reason="gap %.2f, need %.3f" % (tgt - cur[s], need(s))))

    # 4. repair items directly before the skill they unblock
    if use_misconceptions:
        for mid in mis.repairs():
            ms = content.misconceptions[mid]["skill"]
            if ms not in closure and not any(ms in g.prereq_closure(t) for t in closure):
                continue
            pos = next((i for i, it in enumerate(seq) if it.skill_id == ms or ms in g.prereq_closure(it.skill_id)), len(seq))
            seq.insert(pos, PlanItem("repair", skill_id=ms, misconception_id=mid, minutes=REPAIR_MIN,
                                     reason="two confident wrong answers on this misconception"))

    # 6. interleave: at most two consecutive sessions of the same skill
    i = 2
    while i < len(seq):
        if seq[i].skill_id == seq[i - 1].skill_id == seq[i - 2].skill_id:
            for j in range(i + 1, len(seq)):
                sj = seq[j].skill_id
                if sj == seq[i].skill_id:
                    continue
                between = {x.skill_id for x in seq[i:j]}
                if seq[j].type == "learn" and (between & g.prereq_closure(sj)):
                    continue
                seq.insert(i, seq.pop(j))
                break
        i += 1

    # day assignment within the stated minutes per day
    start = int(math.ceil(now)) + 1
    cap_day = profile.day_minutes
    used = {}
    day = start
    items, dropped = [], []

    def next_study_day(d):
        while not profile.is_study_day(d, start):
            d += 1
        return d

    day = next_study_day(day)
    for it in seq:
        while used.get(day, 0) + it.minutes > cap_day:
            day = next_study_day(day + 1)
            if day > profile.deadline_day:
                break
        if day > profile.deadline_day:
            it.status, it.reason = "dropped", "not enough hours before the deadline"
            dropped.append(it)
            continue
        it.day = day
        used[day] = used.get(day, 0) + it.minutes
        items.append(it)

    # 5. revision items (spaced by the forgetting curve)
    if use_forgetting and use_revision:
        P = model.params
        events = []   # (day, skill)
        done_day = {}
        for it in items:
            if it.type == "learn":
                done_day[it.skill_id] = it.day
        for s in closure:
            if s in done_day:
                S = max(model.state(s).S, P.s0_days) * ASSUMED_GROWTH
                t0, p = done_day[s], max(tgt, cur[s])
            elif cur[s] > P.revise_below:
                S = model.state(s).S
                t0, p = now, cur[s]
            else:
                continue
            t = t0
            while True:
                dt = S * math.log(p / P.revise_below) if p > P.revise_below else 0.0
                t = t + max(1.0, dt)
                if t > profile.deadline_day:
                    break
                events.append((int(math.ceil(t)), s))
                S *= ASSUMED_GROWTH
                p = max(tgt, p)
        for d0, s in sorted(events):
            d = next_study_day(max(d0, start))
            while used.get(d, 0) + REVISE_MIN > cap_day:
                d = next_study_day(d + 1)
                if d > profile.deadline_day:
                    break
            if d > profile.deadline_day:
                continue
            used[d] = used.get(d, 0) + REVISE_MIN
            items.append(PlanItem("revise", skill_id=s, day=d, minutes=REVISE_MIN, reason="effective mastery will fall below 0.7"))

    items.sort(key=lambda x: (x.day, {"repair": 0, "learn": 1, "revise": 2, "practice": 3}[x.type]))
    n_learn = len({i.skill_id for i in items if i.type == "learn"})
    reason = "%s%d skills to learn, %d repair, %d revisions, %d dropped; target mastery %.1f, %.1f h/week to day %d" % (
        reason_prefix, n_learn, sum(1 for i in items if i.type == "repair"), sum(1 for i in items if i.type == "revise"),
        len(dropped), tgt, profile.hours_per_week, profile.deadline_day)
    return Plan(version=version, created_day=now, reason=reason, items=items, dropped=dropped)


def plan_stability(old, new, k=10):
    """Fraction of the next k items of the old plan (by order) that are no longer among the next k items of the new plan.
    0 means the near-term plan did not change; 1 means it was completely reshuffled."""
    if old is None or not old.items:
        return 0.0
    key = lambda i: (i.type, i.skill_id, i.misconception_id)
    a = [key(i) for i in old.items[:k]]
    b = {key(i) for i in new.items[:k]}
    return sum(1 for x in a if x not in b) / len(a)
