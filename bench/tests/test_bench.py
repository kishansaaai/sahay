import csv, importlib.util, io, sys, tempfile, unittest
from contextlib import redirect_stdout
from pathlib import Path

B = Path(__file__).resolve().parents[1]


def load(name):
    spec = importlib.util.spec_from_file_location(name, B / (name + ".py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m


class Bench(unittest.TestCase):
    def test_thermal_summary_and_svg(self):
        t = load("thermal_chart")
        rows = [{"elapsed_s": float(s), "tok_per_s": 10.0 if s < 600 else 6.0, "thermal": 1.0, "battery_pct": 80.0, "tokens": 100.0} for s in range(0, 1200, 20)]
        s = t.summary(rows)
        self.assertAlmostEqual(s["peak"], 10.0); self.assertAlmostEqual(s["kept"], 0.6)
        with tempfile.TemporaryDirectory() as d:
            t.svg([("x", rows)], d + "/a.svg"); self.assertIn("<polyline", open(d + "/a.svg").read())

    def test_energy_battery_math(self):
        e = load("energy_calc")
        class A: capacity_mah = 4000; start = 80; end = 70; tokens = 10000; idle_pct_per_hour = 0.0; minutes = 20
        buf = io.StringIO()
        with redirect_stdout(buf): e.battery(A)
        j = 0.10 * 4.0 * 3.85 * 3600  # 10% of 4 Ah at 3.85 V
        self.assertIn("energy %.0f J" % j, buf.getvalue())
        self.assertIn("%.1f mJ" % (1000 * j / 10000), buf.getvalue())


if __name__ == "__main__":
    unittest.main()
