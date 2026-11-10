#!/usr/bin/env python3
"""One command reproduces every simulator number in the report (PRD I3, issue C15).

    python simulator/run_all.py                 # 1,000 learners per type (500 per domain), results in simulator/results
    python simulator/run_all.py --quick         # 5 learners per type and domain, smoke test
    python simulator/run_all.py --budget 150    # stop after 150 s; rerun to resume (finished shards are kept)

Learners and attendance are seeded, so reruns give identical numbers. Takes tens of minutes on a laptop.
"""
import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from sahay_sim.experiments import run_all  # noqa: E402
from sahay_sim.report import make_report  # noqa: E402

ap = argparse.ArgumentParser()
ap.add_argument("--n", type=int, default=500, help="learners per type per domain")
ap.add_argument("--quick", action="store_true")
ap.add_argument("--procs", type=int, default=2)
ap.add_argument("--budget", type=float, default=None, help="seconds before pausing (resumable)")
ap.add_argument("--out", default=str(HERE / "results"))
a = ap.parse_args()
n = 5 if a.quick else a.n
out = Path(a.out if not a.quick else HERE / "results_quick")
if run_all(out, n, a.procs, a.budget):
    make_report(out, n * 2, Path(__file__).resolve().parents[1] / "docs" / ("results" if not a.quick else "results_quick"))
