"""Misconception engine (PRD 9.3, feature C3).

- Choosing a wrong option adds evidence to its misconception, weighted by stated confidence:
  a confident wrong answer (>= 70) counts 1.0, a guess (<= 30) counts 0.5, linear between,
  so a confident wrong answer counts about twice as much as a guess.
- Two or more confident hits create a repair item, placed before skills that depend on the misconception's skill.
- Resolved after three correct answers on questions where it was a distractor, spaced over at least two days.
"""
from dataclasses import dataclass, field

CONFIDENT = 70
GUESS = 30


def evidence_weight(confidence):
    if confidence >= CONFIDENT:
        return 1.0
    if confidence <= GUESS:
        return 0.5
    return 0.5 + 0.5 * (confidence - GUESS) / (CONFIDENT - GUESS)


@dataclass
class MState:
    evidence: float = 0.0
    confident_hits: int = 0
    last_triggered: float = None
    resolved: bool = False
    correct_days: list = field(default_factory=list)


class MisconceptionEngine:
    REPAIR_HITS = 2
    RESOLVE_CORRECT = 3
    RESOLVE_SPAN_DAYS = 2.0

    def __init__(self):
        self.state = {}

    def _s(self, mid):
        return self.state.setdefault(mid, MState())

    def observe(self, question, chosen_index, confidence, now):
        """Record one answer to a question dict from the bank."""
        correct = question["options"][chosen_index]["misconception"] is None
        if correct:
            # credit every misconception that appeared as a distractor on this question
            for mid in sorted({o["misconception"] for o in question["options"] if o["misconception"]}):
                st = self._s(mid)
                if st.confident_hits >= self.REPAIR_HITS or st.evidence > 0:
                    st.correct_days.append(now)
                    self._maybe_resolve(st)
            return
        mid = question["options"][chosen_index]["misconception"]
        st = self._s(mid)
        st.evidence += evidence_weight(confidence)
        if confidence >= CONFIDENT:
            st.confident_hits += 1
        st.last_triggered = now
        st.correct_days = []      # a relapse restarts the resolution count
        st.resolved = False

    def _maybe_resolve(self, st):
        d = st.correct_days
        if len(d) >= self.RESOLVE_CORRECT and (d[-1] - d[-self.RESOLVE_CORRECT]) >= self.RESOLVE_SPAN_DAYS:
            st.resolved = True

    def repairs(self):
        """Misconception ids that currently need a repair item."""
        return sorted(m for m, s in self.state.items() if s.confident_hits >= self.REPAIR_HITS and not s.resolved)

    def active(self):
        return sorted(m for m, s in self.state.items() if s.evidence > 0 and not s.resolved)

    def is_resolved(self, mid):
        return mid in self.state and self.state[mid].resolved
