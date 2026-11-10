"""Prints the same fixed scenario as engine/src/test/kotlin/.../Parity.kt so the two implementations can be diffed."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from sahay_engine import *  # noqa: E402

c = Content.load("dsa")
prof = Profile(hours_per_week=8.0, deadline_day=60)
t = ["dsa.dp_2d", "dsa.topo"]
m, mis = LearnerModel(), MisconceptionEngine()
q = c.by_skill["dsa.arrays"][0]
wrong = next(i for i, o in enumerate(q["options"]) if o["misconception"])
mis.observe(q, wrong, 90, 0.0); mis.observe(q, wrong, 90, 0.5)
for s in ("dsa.complexity", "dsa.stack"):
    st = m.state(s); st.p, st.S, st.last_seen = 0.9, 6.0, 0.0
plan = build_plan(c, m, mis, prof, 3.0, t)
for i in plan.items:
    print("plan", i.type, i.skill_id, i.day, i.minutes)
rec = recover(c, m, prof, 3.0, t, plan, 400, mis=mis)
for i in rec.items:
    print("rec", i.type, i.skill_id, i.day, i.minutes)
for i in rec.dropped:
    print("drop", i.skill_id, i.minutes, i.reason)
