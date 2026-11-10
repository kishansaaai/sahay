"""Cognitive-load-aware pacing (PRD 9.7, feature X1). On-device signals only; always dismissible."""
from statistics import median

LIGHTER, BREAK, END_SESSION, NONE = "lighter_activity", "break", "end_session_with_recap", "none"


class FatigueMonitor:
    WINDOW = 5
    THRESHOLD = 0.6          # ASSUMPTION: tuned later with the simulator and pilot

    def __init__(self):
        self.rts, self.results = [], []

    def add(self, response_ms, correct):
        self.rts.append(response_ms)
        self.results.append(bool(correct))

    def error_streak(self):
        n = 0
        for r in reversed(self.results):
            if r:
                break
            n += 1
        return n

    def index(self):
        """0..1: 0.6 * rise of recent response time against the learner's own median + 0.4 * error streak."""
        if len(self.rts) < self.WINDOW + 3:
            return 0.0
        base = median(self.rts[:-self.WINDOW])
        recent = median(self.rts[-self.WINDOW:])
        rise = min(1.0, max(0.0, recent / base - 1.0)) if base > 0 else 0.0
        streak = min(1.0, self.error_streak() / 4.0)
        return 0.6 * rise + 0.4 * streak

    def suggestion(self):
        f = self.index()
        if f < self.THRESHOLD:
            act = NONE
        elif f < 0.75:
            act = LIGHTER
        elif f < 0.9:
            act = BREAK
        else:
            act = END_SESSION
        return {"fatigue": round(f, 3), "action": act, "dismissible": True}   # never blocks the learner
