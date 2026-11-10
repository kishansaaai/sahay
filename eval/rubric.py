#!/usr/bin/env python3
"""Human-review scripts (PRD 12.3).

    python eval/rubric.py sample --run base-1b-int4            # writes review CSVs under eval/results/<run>/review/
    python eval/rubric.py score --a reviewer1.csv --b reviewer2.csv
    python eval/rubric.py hindi-score --a hi1.csv --b hi2.csv

Explanation rubric: four criteria scored 0 or 1 each (correct, clear, grounded, appropriate level) = 0..4, two reviewers, 100 samples.
Hindi review: 50 explanation pairs (English original, Hindi output), two native speakers score faithful, fluent, natural (0..1 each).
Teach-back: 40 learner explanations for a human grader (0..3 on accuracy, completeness, clarity, example); agreement via tutor.agreement.
"""
import argparse
import csv
import json
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
RESULTS = Path(__file__).with_name("results")
EXPL = ["correct", "clear", "grounded", "level"]
HI = ["faithful", "fluent", "natural"]


def _rows(run, task):
    with open(RESULTS / run / "outputs.jsonl", encoding="utf-8") as f:
        return [r for r in map(json.loads, f) if r["task"] == task and r["output"] and r["source"] != "fallback"]


def sample(run, seed=7):
    rnd = random.Random(seed)
    out = RESULTS / run / "review"
    out.mkdir(parents=True, exist_ok=True)
    ex = _rows(run, "explain")
    pick = rnd.sample(ex, min(100, len(ex)))
    with open(out / "explanations.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "skill_id", "language", "question", "explanation", "analogy", "check_question"] + EXPL)
        for i, r in enumerate(pick):
            o = r["output"]
            w.writerow([i, o["skill_id"], r["language"], r["input"]["question"], o["explanation"], o["analogy"], o["check_question"], "", "", "", ""])
    en = {r["pair"]: r for r in ex if r["language"] == "en"}
    hi = {r["pair"]: r for r in ex if r["language"] == "hi"}
    pairs = sorted(set(en) & set(hi))
    pairs = rnd.sample(pairs, min(50, len(pairs)))
    with open(out / "hindi_pairs.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "english_original", "hindi_output"] + HI)
        for i, p in enumerate(pairs):
            w.writerow([i, en[p]["output"]["explanation"], hi[p]["output"]["explanation"], "", "", ""])
    tb = _rows(run, "teachback_grade")
    pick = rnd.sample(tb, min(40, len(tb)))
    with open(out / "teachback_for_human.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "skill_id", "learner_text", "human_accuracy", "human_completeness", "human_clarity", "human_example", "model_accuracy", "model_completeness", "model_clarity", "model_example"])
        for i, r in enumerate(pick):
            s = r["output"]["scores"]
            w.writerow([i, r["output"]["skill_id"], r["input"]["learner_text"], "", "", "", "", s["accuracy"], s["completeness"], s["clarity"], s["example"]])
    print("wrote review sheets to", out)


def _load(path, cols):
    with open(path, newline="", encoding="utf-8") as f:
        return [[int(r[c]) for c in cols] for r in csv.DictReader(f)]


def kappa(a, b):
    """Cohen's kappa for two raters on binary labels."""
    n = len(a)
    po = sum(x == y for x, y in zip(a, b)) / n
    pa, pb = sum(a) / n, sum(b) / n
    pe = pa * pb + (1 - pa) * (1 - pb)
    return 1.0 if pe == 1 else (po - pe) / (1 - pe)


def score(pa, pb, cols):
    A, B = _load(pa, cols), _load(pb, cols)
    assert len(A) == len(B), "reviewers rated a different number of rows"
    print("rows: %d" % len(A))
    for j, c in enumerate(cols):
        x, y = [r[j] for r in A], [r[j] for r in B]
        print("%-10s mean A %.2f  mean B %.2f  agreement %.2f  kappa %.2f" % (c, sum(x) / len(x), sum(y) / len(y), sum(i == k for i, k in zip(x, y)) / len(x), kappa(x, y)))
    tot = [(sum(a) + sum(b)) / (2 * len(cols)) for a, b in zip(A, B)]
    print("overall mean (0..1 per criterion): %.2f" % (sum(tot) / len(tot)))


def teachback_agreement(path, tolerance=0):
    from llm.sahay_llm import tutor
    H, M = [], []
    with open(path, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            k = ["accuracy", "completeness", "clarity", "example"]
            H.append({x: int(r["human_" + x]) for x in k})
            M.append({x: int(r["model_" + x]) for x in k})
    print("teach-back agreement (all four within %d point): %.2f over %d samples (target 0.70)" % (tolerance, tutor.agreement(M, H, tolerance), len(H)))


def main():
    ap = argparse.ArgumentParser()
    s = ap.add_subparsers(dest="cmd", required=True)
    p = s.add_parser("sample"); p.add_argument("--run", required=True)
    for n in ("score", "hindi-score"):
        p = s.add_parser(n); p.add_argument("--a", required=True); p.add_argument("--b", required=True)
    p = s.add_parser("teachback"); p.add_argument("--csv", required=True); p.add_argument("--tolerance", type=int, default=0)
    a = ap.parse_args()
    if a.cmd == "sample": sample(a.run)
    elif a.cmd == "score": score(a.a, a.b, EXPL)
    elif a.cmd == "hindi-score": score(a.a, a.b, HI)
    else: teachback_agreement(a.csv, a.tolerance)


if __name__ == "__main__":
    main()
