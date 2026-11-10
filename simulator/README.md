# Learner simulator and evaluation (PRD 12.1, issues C12, C13, C15)

Headless, seeded, uses the same engine module as the rest of the repo (`engine/sahay_engine`).

    python simulator/run_all.py                # all experiments, 1,000 learners per type, report in docs/results/
    python simulator/run_all.py --quick        # smoke test
    python simulator/run_all.py --budget 150   # pause after 150 s and rerun to resume
    python simulator/diagnostic_check.py       # diagnostic length, duration and placement (docs/results/diagnostic.md)
    python -m unittest discover -s simulator/tests

Needs numpy and matplotlib for the report (`pip install numpy matplotlib`). Raw shards go to `simulator/results/`
(git-ignored, regenerated deterministically); tables and charts are committed under `docs/results/`.

Learner types: fast, average, slow, forgetful, overconfident, anxious, irregular, misconception-prone.
Conditions: full planner; baselines fixed linear roadmap, topic-only, no forgetting curve, random order; ablations
no re-planning, no diagnostic. **Not run:** the LLM-only planner baseline, which needs the real on-device model (Stream A).
All learner parameters are assumptions; read `docs/results/results.md` with that caveat.
