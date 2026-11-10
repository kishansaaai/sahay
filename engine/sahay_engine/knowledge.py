"""Knowledge tracing with forgetting (PRD 9.1 and 9.2, feature C4).

BKT posterior:   correct: p' = p(1-s) / (p(1-s) + (1-p)g)      wrong: p' = ps / (ps + (1-p)(1-g))
Learning step:   p_next = p' + (1-p') t
Hints:           the update is scaled toward the prior by weight w in (1.0, 0.7, 0.4, 0) for hint levels 0..3
Forgetting:      R = exp(-dt / S), p_eff = p * R
Stability:       success -> S *= growth in [1.5, 2.5] scaling with confidence and speed;
                 failure -> S = max(1 day, 0.5 S)
Revision:        scheduled when p_eff < 0.7
"""
import math
from dataclasses import dataclass, field


@dataclass
class Params:
    p0: float = 0.2          # initial mastery (PRD default)
    slip: float = 0.10       # PRD default
    learn: float = 0.15      # PRD default
    s0_days: float = 2.0     # ASSUMPTION: initial stability, not given in the PRD
    revise_below: float = 0.7
    hint_weights: tuple = (1.0, 0.7, 0.4, 0.0)
    growth_min: float = 1.5
    growth_max: float = 2.5
    fail_factor: float = 0.5
    s_min_days: float = 1.0
    expected_ms: float = 45000.0   # ASSUMPTION: reference response time for the speed factor


def bkt_update(p, correct, slip, guess, learn, weight=1.0):
    if correct:
        num = p * (1 - slip)
        den = num + (1 - p) * guess
    else:
        num = p * slip
        den = num + (1 - p) * (1 - guess)
    post = num / den if den > 0 else p
    nxt = post + (1 - post) * learn
    new = p + weight * (nxt - p)          # scale the update toward the prior
    return min(1.0, max(0.0, new))


def retention(dt, stability):
    return math.exp(-max(dt, 0.0) / stability)


@dataclass
class SkillState:
    p: float
    S: float
    last_seen: float = None
    attempts: int = 0
    confidence_bias: float = 0.0


@dataclass
class LearnerModel:
    params: Params = field(default_factory=Params)
    skills: dict = field(default_factory=dict)

    def state(self, sid):
        if sid not in self.skills:
            self.skills[sid] = SkillState(p=self.params.p0, S=self.params.s0_days)
        return self.skills[sid]

    def p_eff(self, sid, now):
        st = self.state(sid)
        if st.last_seen is None:
            return st.p
        return st.p * retention(now - st.last_seen, st.S)

    def observe(self, sid, correct, confidence, response_ms, hint_level, now, n_options=4):
        """Update mastery and stability after one answer. confidence is 0..100."""
        P, st = self.params, self.state(sid)
        w = P.hint_weights[min(max(hint_level, 0), 3)]
        st.p = bkt_update(st.p, correct, P.slip, 1.0 / n_options, P.learn, w)
        if w > 0:
            if correct:
                conf = min(max(confidence, 0), 100) / 100.0
                speed = min(1.0, P.expected_ms / max(response_ms, 1.0))
                growth = P.growth_min + (P.growth_max - P.growth_min) * (0.5 * conf + 0.5 * speed)
                st.S *= 1.0 + (growth - 1.0) * w      # hints dilute the stability gain
            else:
                st.S = max(P.s_min_days, P.fail_factor * st.S)
        st.last_seen = now
        st.attempts += 1
        return st.p

    def needs_revision(self, sid, now):
        st = self.state(sid)
        return st.last_seen is not None and self.p_eff(sid, now) < self.params.revise_below
