import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sahay_sim.run import run, CONDITIONS, auc   # noqa: E402


class TestSim(unittest.TestCase):
    def test_deterministic_with_fixed_seeds(self):
        a = run("dsa", "average", 3, "full"); b = run("dsa", "average", 3, "full")
        self.assertEqual(a, b)

    def test_every_condition_runs_and_metrics_are_sane(self):
        for c in CONDITIONS:
            r = run("mleng", "misconception", 1, c)
            self.assertTrue(0.0 <= r["mean_k_end"] <= 1.0 and 0.0 <= r["retention30"] <= 1.0 and r["hours"] >= 0)

    def test_full_planner_never_violates_prerequisites(self):
        for t in ("fast", "slow", "irregular", "misconception"):
            for i in range(10):
                self.assertEqual(run("dsa", t, i, "full")["prereq_violations"], 0)

    def test_random_order_does_violate(self):
        self.assertTrue(sum(run("dsa", "average", i, "random_order")["prereq_violations"] for i in range(5)) > 0)

    def test_auc_reference(self):
        self.assertEqual(auc([(0.9, True), (0.8, True), (0.2, False), (0.1, False)]), 1.0)
        self.assertEqual(auc([(0.5, True), (0.5, False)]), 0.5)
        self.assertEqual(auc([(0.1, True), (0.9, False)]), 0.0)


if __name__ == "__main__":
    unittest.main()
