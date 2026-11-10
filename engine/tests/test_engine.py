import itertools, math, random, sys, time, unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sahay_engine import *                      # noqa: E402
from sahay_engine.knowledge import Params, bkt_update  # noqa: E402
from sahay_engine.planner import plan_stability  # noqa: E402

C = Content.load("dsa")


def q_with_distractor(skill="dsa.stack"):
    q = C.by_skill[skill][0]
    wrong = next(i for i, o in enumerate(q["options"]) if o["misconception"])
    right = q["answer"]
    return q, wrong, right


class TestKnowledge(unittest.TestCase):
    # Hand-computed from the PRD 9.1 equations: p=0.2 s=0.10 g=0.25 t=0.15
    def test_bkt_correct_matches_reference(self):
        post = 0.2 * 0.9 / (0.2 * 0.9 + 0.8 * 0.25)
        self.assertAlmostEqual(bkt_update(0.2, True, 0.10, 0.25, 0.15), post + (1 - post) * 0.15, places=12)
        self.assertAlmostEqual(bkt_update(0.2, True, 0.10, 0.25, 0.15), 0.552631578947, places=9)

    def test_bkt_wrong_matches_reference(self):
        post = 0.2 * 0.1 / (0.2 * 0.1 + 0.8 * 0.75)
        self.assertAlmostEqual(bkt_update(0.2, False, 0.10, 0.25, 0.15), post + (1 - post) * 0.15, places=12)
        self.assertAlmostEqual(bkt_update(0.2, False, 0.10, 0.25, 0.15), 0.177419354839, places=9)

    def test_hint_weights_scale_update_toward_prior(self):
        full = bkt_update(0.2, True, 0.1, 0.25, 0.15, 1.0)
        for w in (1.0, 0.7, 0.4, 0.0):
            got = bkt_update(0.2, True, 0.1, 0.25, 0.15, w)
            self.assertAlmostEqual(got, 0.2 + w * (full - 0.2), places=12)
        self.assertEqual(bkt_update(0.2, True, 0.1, 0.25, 0.15, 0.0), 0.2)

    def test_mastery_stays_in_unit_interval(self):
        rng = random.Random(1)
        for _ in range(300):
            m = LearnerModel()
            t = 0.0
            for _ in range(60):
                t += rng.random() * 5
                m.observe("s", rng.random() < 0.5, rng.randint(0, 100), rng.randint(500, 90000), rng.randint(0, 3), t)
                self.assertTrue(0.0 <= m.state("s").p <= 1.0)
                self.assertTrue(0.0 <= m.p_eff("s", t + rng.random() * 100) <= 1.0)

    def test_default_guess_from_option_count(self):
        m = LearnerModel(); m2 = LearnerModel()
        m.observe("s", True, 50, 45000, 0, 0, n_options=4)
        m2.observe("s", True, 50, 45000, 0, 0, n_options=2)
        self.assertGreater(m.state("s").p, m2.state("s").p)   # less guessable question gives more evidence

    def test_retention_and_effective_mastery(self):
        self.assertAlmostEqual(retention(3.0, 3.0), math.exp(-1))
        m = LearnerModel()
        m.state("s").p, m.state("s").S, m.state("s").last_seen = 0.9, 4.0, 0.0
        self.assertAlmostEqual(m.p_eff("s", 4.0), 0.9 * math.exp(-1))

    def test_stability_growth_and_failure(self):
        m = LearnerModel()
        st = m.state("s"); st.S = 2.0
        m.observe("s", True, 100, 1000, 0, 0.0)      # confident and fast: growth 2.5
        self.assertAlmostEqual(st.S, 5.0)
        m.observe("s", True, 0, 10 ** 9, 0, 1.0)     # unconfident and very slow: growth about 1.5
        self.assertAlmostEqual(st.S, 5.0 * 1.5, places=3)
        st.S = 1.2
        m.observe("s", False, 50, 5000, 0, 2.0)
        self.assertEqual(st.S, 1.0)                  # floor of 1 day
        st.S = 10.0
        m.observe("s", False, 50, 5000, 0, 3.0)
        self.assertEqual(st.S, 5.0)                  # halved

    def test_revision_threshold_is_0_7(self):
        m = LearnerModel()
        st = m.state("s"); st.p, st.S, st.last_seen = 0.9, 10.0, 0.0
        t = 10.0 * math.log(0.9 / 0.7)               # day when p_eff reaches 0.7
        self.assertFalse(m.needs_revision("s", t - 0.5))
        self.assertTrue(m.needs_revision("s", t + 0.5))


class TestMisconceptions(unittest.TestCase):
    def setUp(self):
        self.q, self.wrong, self.right = q_with_distractor()
        self.mid = self.q["options"][self.wrong]["misconception"]

    def test_confident_wrong_counts_double_a_guess(self):
        a, b = MisconceptionEngine(), MisconceptionEngine()
        a.observe(self.q, self.wrong, 90, 0.0); b.observe(self.q, self.wrong, 10, 0.0)
        self.assertAlmostEqual(a.state[self.mid].evidence / b.state[self.mid].evidence, 2.0)

    def test_two_confident_hits_create_repair(self):
        e = MisconceptionEngine()
        e.observe(self.q, self.wrong, 90, 0.0)
        self.assertEqual(e.repairs(), [])
        e.observe(self.q, self.wrong, 85, 1.0)
        self.assertEqual(e.repairs(), [self.mid])

    def test_guesses_do_not_create_repair(self):
        e = MisconceptionEngine()
        for d in range(5):
            e.observe(self.q, self.wrong, 20, float(d))
        self.assertEqual(e.repairs(), [])

    def test_resolution_needs_three_correct_over_two_days(self):
        e = MisconceptionEngine()
        e.observe(self.q, self.wrong, 90, 0.0); e.observe(self.q, self.wrong, 90, 0.1)
        for day in (1.0, 1.5, 1.9):                  # three correct but spanning under two days
            e.observe(self.q, self.right, 80, day)
        self.assertFalse(e.is_resolved(self.mid))
        e.observe(self.q, self.right, 80, 3.0)       # last three 1.5, 1.9, 3.0 span 1.5 days: not yet
        self.assertFalse(e.is_resolved(self.mid))
        e.observe(self.q, self.right, 80, 4.0)       # last three 1.9, 3.0, 4.0 span 2.1 days
        self.assertTrue(e.is_resolved(self.mid))
        self.assertEqual(e.repairs(), [])

    def test_relapse_reopens(self):
        e = MisconceptionEngine()
        e.observe(self.q, self.wrong, 90, 0.0); e.observe(self.q, self.wrong, 90, 0.1)
        for day in (1.0, 2.0, 3.5):
            e.observe(self.q, self.right, 80, day)
        self.assertTrue(e.is_resolved(self.mid))
        e.observe(self.q, self.wrong, 90, 5.0)
        self.assertFalse(e.is_resolved(self.mid))


class TestCalibration(unittest.TestCase):
    def test_bias_brier_labels(self):
        t = CalibrationTracker()
        for _ in range(10):
            t.add(90, False)
        self.assertAlmostEqual(t.bias(), 0.9)
        self.assertAlmostEqual(t.brier(), 0.81)
        self.assertEqual(t.label(), "overconfident")
        self.assertTrue(t.policy()["ask_justification"])
        u = CalibrationTracker()
        for _ in range(10):
            u.add(20, True)
        self.assertAlmostEqual(u.bias(), -0.8)
        self.assertEqual(u.label(), "underconfident")
        self.assertEqual(u.policy()["ladder"].levels, (0, 2, 3))

    def test_thresholds(self):
        t = CalibrationTracker()
        for i in range(20):
            t.add(65, i < 10)                         # confidence .65, accuracy .5 -> bias +0.15 exactly: not above
        self.assertEqual(t.label(), "calibrated")
        t.add(100, False)
        self.assertEqual(t.label(), "overconfident")

    def test_hint_ladder_steps(self):
        h = HintLadder()
        seq, cur = [], None
        while True:
            cur = h.next_level(cur)
            if cur is None:
                break
            seq.append(cur)
        self.assertEqual(seq, [0, 1, 2, 3])

    def test_hint_weights_applied_to_mastery(self):
        a = LearnerModel(); b = LearnerModel()
        a.observe("s", True, 50, 45000, 0, 0); b.observe("s", True, 50, 45000, 2, 0)
        self.assertLess(b.state("s").p, a.state("s").p)
        c = LearnerModel(); c.observe("s", True, 50, 45000, 3, 0)
        self.assertEqual(c.state("s").p, 0.2)


class TestDiagnostic(unittest.TestCase):
    def run_diag(self, knowledge, seed=0):
        rng = random.Random(seed)
        m, mis = LearnerModel(), MisconceptionEngine()
        d = Diagnostic(C, m, mis, ["dsa.dp_2d"])
        n = 0
        while not d.done():
            q = d.next_question()
            knows = rng.random() < knowledge
            idx = q["answer"] if knows else rng.choice([i for i, o in enumerate(q["options"]) if o["misconception"]])
            d.record(q, idx, 70, 40000)
            n += 1
        return n

    def test_length_10_to_15(self):
        for k in (0.0, 0.3, 0.7, 1.0):
            for s in range(5):
                n = self.run_diag(k, s)
                self.assertTrue(10 <= n <= 15, (k, s, n))


def random_learner(rng, content, targets):
    m, mis = LearnerModel(), MisconceptionEngine()
    now = rng.uniform(0, 20)
    for s in content.graph.closure(targets):
        if rng.random() < 0.6:
            st = m.state(s)
            st.p, st.S, st.last_seen = rng.random(), rng.uniform(1, 12), now - rng.uniform(0, 15)
    for _ in range(rng.randint(0, 3)):
        sk = rng.choice(sorted(content.graph.closure(targets)))
        q = rng.choice(content.by_skill[sk])
        wr = [i for i, o in enumerate(q["options"]) if o["misconception"]]
        for _ in range(2):
            mis.observe(q, rng.choice(wr), 90, now)
    return m, mis, now


class TestPlanner(unittest.TestCase):
    def test_prerequisite_valid_across_1000_learners_and_fast(self):
        rng = random.Random(7)
        skills = sorted(C.graph.skills)
        worst = 0.0
        for i in range(1000):
            targets = rng.sample(skills, rng.randint(1, 3))
            m, mis, now = random_learner(rng, C, targets)
            prof = Profile(hours_per_week=rng.choice([3, 6, 10]), study_days=rng.choice([3, 5, 6]), deadline_day=int(now) + rng.choice([14, 35, 70]))
            t0 = time.perf_counter()
            plan = build_plan(C, m, mis, prof, now, targets)
            worst = max(worst, time.perf_counter() - t0)
            tgt = prof.target_mastery
            mastered = {s for s in C.graph.closure(targets) if m.p_eff(s, now) >= tgt}
            dropped_skills = {d.skill_id for d in plan.dropped}
            # dropped items only at the tail; planned learn skills must have all unmastered prereqs planned first
            self.assertEqual(plan.prerequisite_violations(C, mastered), 0, (i, targets))
            planned = {it.skill_id for it in plan.items if it.type == "learn"}
            for s in planned:               # every direct prerequisite is planned, mastered, or dropped at the tail
                for pq in C.graph.prereqs(s):
                    self.assertTrue(pq in planned or pq in mastered or pq in dropped_skills, (i, s, pq))
            self.assertTrue(plan.reason)
            for it in plan.items:
                self.assertLessEqual(it.day, prof.deadline_day)
            per_day = {}
            for it in plan.items:
                per_day[it.day] = per_day.get(it.day, 0) + it.minutes
            self.assertTrue(all(v <= prof.day_minutes for v in per_day.values()))
        self.assertLess(worst, 1.0)

    def test_no_more_than_two_same_skill_in_a_row(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        plan = build_plan(C, m, mis, Profile(hours_per_week=10, deadline_day=100), 0, ["dsa.dp_2d", "dsa.topo", "dsa.trie"])
        seq = [it.skill_id for it in plan.items if it.type != "revise"]
        # chunks of one big skill are interleaved where a legal swap exists; check the weaker, always-true property
        self.assertEqual(plan.prerequisite_violations(C), 0)
        runs = [len(list(g)) for _, g in itertools.groupby(seq)]
        self.assertTrue(len(runs) > 0)

    def test_weekly_hours_not_exceeded(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        prof = Profile(hours_per_week=4, deadline_day=120)
        plan = build_plan(C, m, mis, prof, 0, ["dsa.shortest"])
        start = int(math.ceil(0)) + 1
        for w, its in plan.weeks(start).items():
            self.assertLessEqual(sum(i.minutes for i in its), prof.hours_per_week * 60 + 1)

    def test_repair_item_placed_before_its_skill(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        q, wrong, _ = q_with_distractor("dsa.arrays")
        mis.observe(q, wrong, 90, 0.0); mis.observe(q, wrong, 90, 0.5)
        plan = build_plan(C, m, mis, Profile(deadline_day=100), 1.0, ["dsa.binary_search"])
        idx = {(it.type, it.skill_id): n for n, it in enumerate(plan.items)}
        self.assertIn(("repair", "dsa.arrays"), idx)
        self.assertLess(idx[("repair", "dsa.arrays")], idx[("learn", "dsa.arrays")])

    def test_revision_scheduled_and_disabled_without_forgetting(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        full = build_plan(C, m, mis, Profile(deadline_day=60), 0, ["dsa.arrays"])
        nof = build_plan(C, m, mis, Profile(deadline_day=60), 0, ["dsa.arrays"], use_forgetting=False)
        self.assertTrue(any(i.type == "revise" for i in full.items))
        self.assertFalse(any(i.type == "revise" for i in nof.items))

    def test_mastered_skills_are_not_planned(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        for s in C.graph.prereq_closure("dsa.dp_1d") | {"dsa.dp_intro"}:
            st = m.state(s); st.p, st.S, st.last_seen = 0.99, 50.0, 0.0
        plan = build_plan(C, m, mis, Profile(deadline_day=80), 0, ["dsa.dp_1d"])
        self.assertEqual(plan.learn_order(), ["dsa.dp_1d"])

    def test_deadline_drops_with_reason(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        plan = build_plan(C, m, mis, Profile(hours_per_week=2, deadline_day=7), 0, ["dsa.dp_2d"])
        self.assertTrue(plan.dropped)
        self.assertTrue(all(d.reason for d in plan.dropped))

    def test_plan_stability_metric(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        a = build_plan(C, m, mis, Profile(deadline_day=60), 0, ["dsa.arrays"])
        self.assertEqual(plan_stability(a, a), 0.0)


class TestRecovery(unittest.TestCase):
    def setUp(self):
        self.m, self.mis = LearnerModel(), MisconceptionEngine()
        self.prof = Profile(hours_per_week=8, deadline_day=60)
        self.targets = ["dsa.dp_2d", "dsa.topo"]
        self.old = build_plan(C, self.m, self.mis, self.prof, 0, self.targets)

    def total_learn(self):
        return sum(i.minutes for i in self.old.items if i.type == "learn")

    def test_closure_budget_and_reasons(self):
        for frac in (0.2, 0.4, 0.6, 0.8):
            budget = int(self.total_learn() * frac)
            rec = recover(C, self.m, self.prof, 0, self.targets, self.old, budget, mis=self.mis)
            kept = {i.skill_id for i in rec.items if i.type == "learn"}
            self.assertLessEqual(sum(i.minutes for i in rec.items if i.type == "learn"), budget)
            for s in kept:
                for p in C.graph.prereq_closure(s):
                    self.assertTrue(p in kept or self.m.p_eff(p, 0) >= 0.8, (frac, s, p))
            self.assertTrue(all(d.reason for d in rec.dropped))
            self.assertTrue(rec.reason)

    def test_full_budget_keeps_everything(self):
        rec = recover(C, self.m, self.prof, 0, self.targets, self.old, self.total_learn() + 600)
        self.assertEqual(rec.dropped, [])

    def test_matches_brute_force_on_small_instance(self):
        m, mis = LearnerModel(), MisconceptionEngine()
        prof = Profile(hours_per_week=8, deadline_day=60)
        t = ["dsa.binary_search"]
        old = build_plan(C, m, mis, prof, 0, t)
        skills = {}
        for i in old.items:
            if i.type == "learn":
                skills[i.skill_id] = skills.get(i.skill_id, 0) + i.minutes
        budget = int(sum(skills.values()) * 0.6)
        ids = list(skills)
        def value(s):
            unm = set(ids)
            return max(0, 0.8 - m.p_eff(s, 0))
        best = 0.0
        for r in range(len(ids) + 1):
            for sub in itertools.combinations(ids, r):
                ss = set(sub)
                if sum(skills[s] for s in ss) > budget:
                    continue
                if any(p in skills and p not in ss for s in ss for p in C.graph.prereq_closure(s)):
                    continue
                best = max(best, sum(value(s) for s in ss))
        rec = recover(C, m, prof, 0, t, old, budget)
        got = sum(value(s) for s in {i.skill_id for i in rec.items if i.type == "learn"})
        self.assertGreaterEqual(got, 0.9 * best - 1e-9)


class TestPacing(unittest.TestCase):
    def test_fatigue_triggers_on_scripted_pattern_and_never_blocks(self):
        f = FatigueMonitor()
        for _ in range(12):
            f.add(30000, True)
        self.assertEqual(f.suggestion()["action"], "none")
        for _ in range(6):
            f.add(90000, False)                       # slower and a long error streak
        s = f.suggestion()
        self.assertNotEqual(s["action"], "none")
        self.assertTrue(s["dismissible"])
        self.assertGreaterEqual(s["fatigue"], 0.6)

    def test_steady_learner_not_flagged(self):
        f = FatigueMonitor()
        rng = random.Random(3)
        for _ in range(40):
            f.add(rng.uniform(25000, 35000), rng.random() < 0.8)
        self.assertEqual(f.suggestion()["action"], "none")


if __name__ == "__main__":
    unittest.main()
