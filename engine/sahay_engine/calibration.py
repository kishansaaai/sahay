"""Hint ladder and confidence calibration (PRD 9.4, feature I1).

Ladder levels: 0 nudge, 1 hint, 2 worked step, 3 solution. Mastery weights 1.0, 0.7, 0.4, 0 are applied in
knowledge.LearnerModel.observe. Calibration: bias = mean(confidence) - accuracy, plus the Brier score.
bias > +0.15 -> overconfident (tutor asks for a justification); bias < -0.15 -> underconfident
(encouragement and a shorter ladder).
"""
from dataclasses import dataclass

LEVEL_NAMES = ("nudge", "hint", "worked_step", "solution")
OVER, UNDER, OK = "overconfident", "underconfident", "calibrated"


@dataclass
class HintLadder:
    levels: tuple = (0, 1, 2, 3)

    def next_level(self, current):
        """current is the last level shown, or None before any hint. Returns the next level or None when exhausted."""
        for lv in self.levels:
            if current is None or lv > current:
                return lv
        return None


class CalibrationTracker:
    MIN_ANSWERS = 5          # ASSUMPTION: avoid labelling on too little evidence
    BIAS_LIMIT = 0.15

    def __init__(self):
        self.conf, self.hit = [], []

    def add(self, confidence_0_100, correct):
        self.conf.append(min(max(confidence_0_100, 0), 100) / 100.0)
        self.hit.append(1.0 if correct else 0.0)

    @property
    def n(self):
        return len(self.conf)

    def bias(self):
        return (sum(self.conf) - sum(self.hit)) / self.n if self.n else 0.0

    def brier(self):
        return sum((c - h) ** 2 for c, h in zip(self.conf, self.hit)) / self.n if self.n else 0.0

    def label(self):
        if self.n < self.MIN_ANSWERS:
            return OK
        b = round(self.bias(), 9)
        return OVER if b > self.BIAS_LIMIT else UNDER if b < -self.BIAS_LIMIT else OK

    def policy(self):
        lab = self.label()
        return {
            "label": lab,
            "ask_justification": lab == OVER,
            "encourage": lab == UNDER,
            "ladder": HintLadder(levels=(0, 2, 3) if lab == UNDER else (0, 1, 2, 3)),
        }
