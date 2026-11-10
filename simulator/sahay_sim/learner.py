"""Digital-twin learner (PRD 12.1): hidden true knowledge, forgetting, misconceptions, noisy answers.

Parameters were chosen a priori and then adjusted ONCE (stability 12 -> 40 days, rate 1.5 -> 2.0) because with the first
values no planner, including a full fixed roadmap, could reach the target in 12 weeks (infeasible, so uninformative).
They were not tuned to favour any planner. Every number here is a MODELLING ASSUMPTION of the simulator, not a measurement of real students. Results show how
planners compare inside this model; they are not evidence about real learners (the human pilot covers that).
"""
import math
import random

BASE = dict(rate=2.0, slip=0.10, guess=None, S=40.0, conf_mode="calibrated", conf_shift=0, skip=0.08, mis=(0, 1), rt=1.0)
TYPES = {
    "fast":          dict(rate=3.2, slip=0.05),
    "average":       dict(),
    "slow":          dict(rate=1.1),
    "forgetful":     dict(S=13.0),
    "overconfident": dict(guess=0.35, conf_mode="over"),
    "anxious":       dict(slip=0.20, conf_mode="anxious", rt=1.4),
    "irregular":     dict(skip=0.45),
    "misconception": dict(mis=(2, 4)),
}
TYPE_NAMES = list(TYPES)
EXPECTED_MIN_PER_Q = 0.75


def params(ltype):
    p = dict(BASE)
    p.update(TYPES[ltype])
    return p


class Learner:
    def __init__(self, content, ltype, seed, targets, prior_fraction=None):
        self.c = content
        self.type = ltype
        self.P = params(ltype)
        self.rng = random.Random(seed)
        g = content.graph
        self.targets = targets
        self.closure = sorted(g.closure(targets))
        rng = self.rng
        f = rng.uniform(0.0, 0.4) if prior_fraction is None else prior_fraction
        self.k, self.peak, self.last, self.S, self.sday = {}, {}, {}, {}, {}
        known = set()
        for s in g.topo_order(self.closure):
            if all(p in known for p in g.prereqs(s)) and rng.random() < f:
                known.add(s)
                self.k[s] = rng.uniform(0.75, 0.95)
            else:
                self.k[s] = rng.uniform(0.0, 0.1)
            self.peak[s] = self.k[s]
            self.last[s] = 0.0
            self.S[s] = self.P["S"]
            self.sday[s] = None
        self.known0 = known
        lo, hi = self.P["mis"]
        n = rng.randint(lo, hi)
        pool = [s for s in self.closure if s not in known]
        self.mis = set()
        for _ in range(n):
            if not pool:
                break
            s = rng.choice(pool)
            ms = [m["id"] for m in content.misconceptions_of(s)]
            self.mis.add(rng.choice(ms))
        self.mis0 = set(self.mis)
        self.minutes = 0.0

    # ---- hidden state -------------------------------------------------
    def k_at(self, s, t):
        return self.k[s] * math.exp(-max(t - self.last[s], 0.0) / self.S[s])

    def mean_k(self, t):
        return sum(self.k_at(s, t) for s in self.closure) / len(self.closure)

    def attends(self, rng=None):
        return (rng or self.rng).random() >= self.P["skip"]

    def has_mis(self, s):
        return [m for m in self.mis if m.rsplit(".", 1)[0] == s]

    def _touch(self, s, t, mult):
        day = int(t)
        if self.sday[s] != day:
            self.sday[s] = day
            self.S[s] = min(self.S[s] * mult, 6.0 * self.P["S"])

    # ---- activities ---------------------------------------------------
    def study(self, s, minutes, t):
        self.k[s] = self.k_at(s, t)
        g = self.c.graph
        rate = self.P["rate"]
        if self.has_mis(s):
            rate *= 0.5
        if any(self.k_at(p, t) < 0.4 for p in g.prereqs(s)):
            rate *= 0.3                                   # unprepared: prerequisites not yet learned
        self.k[s] += (1 - self.k[s]) * (1 - math.exp(-rate * (minutes / 60.0) / g.est_hours(s)))
        self.peak[s] = max(self.peak[s], self.k[s])
        self.last[s] = t
        self._touch(s, t, 1.4)
        for m in self.has_mis(s):
            if self.rng.random() < 0.05 * minutes / 30.0:
                self.mis.discard(m)
        self.minutes += minutes

    def revise(self, s, minutes, t):
        cur = self.k_at(s, t)
        self.k[s] = cur + (self.peak[s] - cur) * 0.8
        self.k[s] += (1 - self.k[s]) * 0.02
        self.peak[s] = max(self.peak[s], self.k[s])
        self.last[s] = t
        self._touch(s, t, 1.5)
        self.minutes += minutes

    def repair(self, s, mid, minutes, t):
        self.k[s] = self.k_at(s, t)
        self.last[s] = t
        if mid in self.mis and self.rng.random() < 0.85:
            self.mis.discard(mid)
        self.minutes += minutes

    # ---- answering ----------------------------------------------------
    def answer(self, q, t):
        """Returns (chosen_index, confidence 0..100, response_ms, correct)."""
        rng, P = self.rng, self.P
        s = q["skill"]
        k = self.k_at(s, t)
        n = len(q["options"])
        guess = P["guess"] if P["guess"] is not None else 1.0 / n
        pc = k * (1 - P["slip"]) + (1 - k) * guess
        # testing effect
        correct = rng.random() < pc
        if correct:
            idx = q["answer"]
        else:
            wrong = [i for i, o in enumerate(q["options"]) if o["misconception"]]
            held = [i for i in wrong if q["options"][i]["misconception"] in self.mis]
            idx = rng.choice(held) if held and rng.random() < 0.75 else rng.choice(wrong)
        mu = 100 * (0.25 + 0.65 * k)
        if (not correct) and held and idx in held:
            mu = max(mu + 25, 70)         # ASSUMPTION: a held misconception is a confidently held error
        if P["conf_mode"] == "over":
            mu = max(mu, 80)
        elif P["conf_mode"] == "anxious":
            mu = max(5, mu - 25)
        conf = int(min(100, max(0, rng.gauss(mu, 10))))
        rt = 40000.0 / (0.5 + k) * P["rt"] * math.exp(rng.gauss(0, 0.25))
        if correct:
            self.k[s] = k + (1 - k) * 0.03
            self.last[s] = t
        else:
            self.k[s] = k
            self.last[s] = t
        return idx, conf, rt, correct
