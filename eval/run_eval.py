#!/usr/bin/env python3
"""LLM evaluation harness (PRD 12.3, issue A16). One command reproduces the LLM numbers in the report.

    python eval/run_eval.py --generator-cmd "<command that reads a Llama-3 prompt on stdin>" --label base-1b-int4
    python eval/run_eval.py --generator mock --label smoke --n 50       # harness smoke test, NOT model numbers
    python eval/run_eval.py --compare base-1b-int4 tuned-1b-int4        # base vs tuned table

For each of the seven tasks it runs N fixed prompts (default 500) through the real orchestrator and reports JSON validity
before retry, after up to two retries, and the fallback rate; it also records latency and saves the raw outputs
(eval/results/<label>/outputs.jsonl) for the rubric scripts in eval/rubric.py.
"""
import argparse
import json
import random
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(Path(__file__).resolve().parent))
import prompt_sets  # noqa: E402
from llm.sahay_llm import fallbacks, prompts  # noqa: E402
from llm.sahay_llm.generators import CommandGenerator  # noqa: E402
from llm.sahay_llm.orchestrator import Orchestrator  # noqa: E402

RESULTS = Path(__file__).with_name("results")


class MockGenerator:
    """Emits the deterministic fallback output, corrupted with probability [error_rate] (and fixed on retry with
    probability 0.7). Exists only to test the harness; its numbers must never be reported as model numbers."""

    def __init__(self, error_rate=0.3, seed=1):
        self.rnd, self.error_rate, self.cur = random.Random(seed), error_rate, None

    def __call__(self, messages, temperature, max_tokens):
        task, inp, lang = self.cur
        retry = len(messages) > 2
        out = fallbacks.FALLBACKS[task](inp, lang)
        if task == "explain":
            words = out["explanation"].split()[:100]
            out.update(explanation=" ".join(words), analogy="Like everyday life." if inp["analogy"] else "")
        if task == "draft_question":
            out = [e for e in prompts.load_task(task)["examples"] if e["lang"] == lang][0]["output"]
        if out is None or self.rnd.random() < (self.error_rate * (0.3 if retry else 1)):
            return "Sure! Here you go: {" + json.dumps(out or {})[1:-8]
        return "```json\n%s\n```" % json.dumps(out, ensure_ascii=False)


def run(label, gen, n, tasks):
    stats_out, rows = {}, []
    out_dir = RESULTS / label
    out_dir.mkdir(parents=True, exist_ok=True)
    for task in tasks:
        orch = Orchestrator(gen)
        items = prompt_sets.build(task, n=n)
        lat = []
        for k, it in enumerate(items):
            if isinstance(gen, MockGenerator):
                gen.cur = (task, it["input"], it["language"])
            r = orch.run(task, it["input"], it["language"])
            lat.append(r.latency_s)
            rows.append({"task": task, "k": k, "language": it["language"], "pair": it.get("pair"), "source": r.source, "attempts": r.attempts,
                         "errors": r.errors, "output": r.output, "input": {x: y for x, y in it["input"].items() if x != "notes"}})
        s = orch.stats.summary()[task]
        lat.sort()
        s["n"] = len(items)
        s["latency_median_s"] = round(lat[len(lat) // 2], 3)
        s["latency_p95_s"] = round(lat[int(len(lat) * 0.95) - 1], 3)
        stats_out[task] = s
        print("%-18s n=%-4d valid before retry %.3f  after retry %.3f  fallback %.3f" % (task, len(items), s["validity_before_retry"], s["validity_after_retry"], s["fallback_rate"]), flush=True)
    with open(out_dir / "outputs.jsonl", "w", encoding="utf-8") as f:
        for r in rows:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    meta = {"label": label, "n_per_task": n, "seed": prompt_sets.SEED, "date": time.strftime("%Y-%m-%d"), "tasks": stats_out,
            "generator": "mock (harness test only)" if isinstance(gen, MockGenerator) else "command"}
    (out_dir / "summary.json").write_text(json.dumps(meta, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    (out_dir / "summary.md").write_text(table(meta), encoding="utf-8")
    return meta


def table(meta):
    s = "# LLM evaluation: %s (%s)\n\nGenerator: %s. Prompts per task: %d, seed %d.\n\n" % (meta["label"], meta["date"], meta["generator"], meta["n_per_task"], meta["seed"])
    s += "| Task | Valid before retry | Valid after retry | Fallback | Median s | p95 s |\n| --- | --- | --- | --- | --- | --- |\n"
    for t, v in meta["tasks"].items():
        s += "| %s | %.1f%% | %.1f%% | %.1f%% | %s | %s |\n" % (t, 100 * v["validity_before_retry"], 100 * v["validity_after_retry"], 100 * v["fallback_rate"], v["latency_median_s"], v["latency_p95_s"])
    return s


def compare(a, b):
    A, B = [json.loads((RESULTS / x / "summary.json").read_text()) for x in (a, b)]
    s = "| Task | %s valid after retry | %s valid after retry | Delta |\n| --- | --- | --- | --- |\n" % (a, b)
    for t in A["tasks"]:
        x, y = A["tasks"][t]["validity_after_retry"], B["tasks"][t]["validity_after_retry"]
        s += "| %s | %.1f%% | %.1f%% | %+.1f pts |\n" % (t, 100 * x, 100 * y, 100 * (y - x))
    print(s + "\nShip the tuned model only if it is better here AND on the explanation rubric; otherwise the base model ships (PRD 10.7).")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--generator", choices=["mock"])
    ap.add_argument("--generator-cmd")
    ap.add_argument("--label", default="run")
    ap.add_argument("--n", type=int, default=prompt_sets.N)
    ap.add_argument("--tasks", nargs="*", default=prompts.TASKS)
    ap.add_argument("--compare", nargs=2)
    a = ap.parse_args()
    if a.compare:
        return compare(*a.compare)
    if a.generator == "mock":
        gen = MockGenerator()
    elif a.generator_cmd:
        gen = CommandGenerator(a.generator_cmd)
    else:
        ap.error("give --generator mock or --generator-cmd")
    run(a.label, gen, a.n, a.tasks)


if __name__ == "__main__":
    main()
