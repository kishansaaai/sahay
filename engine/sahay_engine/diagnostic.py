"""Adaptive diagnostic (PRD 6 C3): 10 to 15 questions chosen to resolve the most uncertainty about the goal skills.

Selection: among goal-closure skills not yet asked, pick the one whose current p is most uncertain
(p(1-p)), breaking ties toward skills with more unmastered dependents; ask a question whose difficulty is
closest to the learner's estimated level. Inference: a correct answer lifts the skill's prerequisites
(they are implied), a wrong one caps its dependents. Misconceptions are recorded for every wrong option.
"""

MIN_Q, MAX_Q = 10, 15
INFER_FLOOR = 0.6          # ASSUMPTION: implied mastery for prerequisites of a skill answered correctly
UNCERTAINTY_STOP = 0.16    # ASSUMPTION: stop after MIN_Q when no skill is more uncertain than this (p(1-p))


class Diagnostic:
    def __init__(self, content, model, mis, targets, seed_difficulty=2, now=0.0):
        self.c, self.model, self.mis, self.now = content, model, mis, now
        self.closure = sorted(content.graph.closure(targets))
        self.asked_skills, self.asked_q, self.history = set(), set(), []
        self.level = seed_difficulty

    def _uncertainty(self, s):
        p = self.model.state(s).p
        return p * (1 - p)

    def done(self):
        n = len(self.history)
        if n >= MAX_Q:
            return True
        if n < MIN_Q:
            return False
        left = [s for s in self.closure if s not in self.asked_skills]
        return not left or max(self._uncertainty(s) for s in left) < UNCERTAINTY_STOP

    def next_question(self):
        if self.done():
            return None
        g = self.c.graph
        cands = [s for s in self.closure if s not in self.asked_skills] or self.closure
        s = max(cands, key=lambda x: (round(self._uncertainty(x), 6), len(g.dependents(x)), x))
        qs = [q for q in self.c.by_skill[s] if q["id"] not in self.asked_q] or self.c.by_skill[s]
        q = min(qs, key=lambda x: (abs(x["difficulty"] - self.level), x["id"]))
        return q

    def record(self, q, chosen_index, confidence, response_ms):
        g = self.c.graph
        correct = q["options"][chosen_index]["misconception"] is None
        self.model.observe(q["skill"], correct, confidence, response_ms, 0, self.now, n_options=len(q["options"]))
        self.mis.observe(q, chosen_index, confidence, self.now)
        self.asked_skills.add(q["skill"])
        self.asked_q.add(q["id"])
        self.history.append((q["id"], correct))
        if correct:
            self.level = min(5, self.level + 1 if q["difficulty"] >= self.level else self.level)
            for p in g.prereq_closure(q["skill"]):
                st = self.model.state(p)
                st.p = max(st.p, INFER_FLOOR)
        else:
            self.level = max(1, self.level - 1)
            sp = self.model.state(q["skill"]).p
            for d in self.closure:
                if q["skill"] in g.prereq_closure(d):
                    st = self.model.state(d)
                    st.p = min(st.p, max(sp, self.model.params.p0))
        return correct
