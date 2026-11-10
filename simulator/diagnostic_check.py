#!/usr/bin/env python3
"""C08 acceptance check: diagnostic length, duration and placement quality on simulated learners.

    python simulator/diagnostic_check.py [--n 100]     (n learners per type and domain)

Placement quality: AUC of the engine's mastery estimate p (after the diagnostic) at separating skills the simulated
learner truly knows (true mastery >= 0.5) from those it does not, averaged over learners.
"""
import argparse, statistics, sys
from pathlib import Path
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from sahay_sim.run import make_learner, content, auc, LearnerModel, MisconceptionEngine, Diagnostic  # noqa: E402
from sahay_sim.learner import TYPE_NAMES  # noqa: E402

ap = argparse.ArgumentParser(); ap.add_argument("--n", type=int, default=100); a = ap.parse_args()
lens, mins, aucs = [], [], []
for d in ("dsa", "mleng"):
    c = content(d)
    for t in TYPE_NAMES:
        for i in range(a.n):
            L = make_learner(d, t, i)
            m, mis = LearnerModel(), MisconceptionEngine()
            dg = Diagnostic(c, m, mis, L.targets, now=0.0)
            n, ms = 0, 0.0
            while not dg.done():
                q = dg.next_question()
                idx, conf, rt, ok = L.answer(q, 0.0)
                dg.record(q, idx, conf, rt)
                n += 1; ms += rt
            lens.append(n); mins.append(ms / 60000.0)
            pairs = [(m.state(s).p, L.k_at(s, 0.0) >= 0.5) for s in L.closure]
            u = auc(pairs)
            if u is not None:
                aucs.append(u)
out = ["# Diagnostic check (C08)", "",
       "%d simulated learners (%d per type and domain, 8 types, 2 domains)." % (len(lens), a.n), "",
       "| Metric | Value | Target |", "| --- | --- | --- |",
       "| Questions asked | mean %.1f, min %d, max %d | 10 to 15 |" % (statistics.mean(lens), min(lens), max(lens)),
       "| Time to finish (simulated response times) | mean %.1f min | under 12 min on average |" % statistics.mean(mins),
       "| Placement AUC (estimate vs true knowledge) | mean %.2f over %d learners | informational |" % (statistics.mean(aucs), len(aucs)), ""]
dest = HERE.parent / "docs" / "results" / "diagnostic.md"
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text("\n".join(out), encoding="utf-8")
print("\n".join(out))
