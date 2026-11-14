#!/usr/bin/env python3
"""Check a LoRA training file before training (issue A18, PRD 10.7).

    python export/finetune/validate_sft.py data/sft.jsonl

Each line: {"task": <one of the 7 tasks>, "language": "en"|"hi", "input": {...}, "output": {...}, "source": "<where it came from>", "licence": "<licence>", "reviewed_by": "<name>"}
Rejects lines whose output fails the task schema and validators (so we only train on strict, valid JSON), hint examples that leak the answer,
lines with no source, licence or reviewer, exact duplicates, and prints the language and task balance.
"""
import collections
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from llm.sahay_llm import prompts, validators  # noqa: E402


def main(path):
    bad, seen, tasks, langs, n = [], set(), collections.Counter(), collections.Counter(), 0
    for ln, line in enumerate(open(path, encoding="utf-8"), 1):
        if not line.strip():
            continue
        n += 1
        try:
            r = json.loads(line)
            inp = dict(r["input"])
            if r["task"] == "hint":
                if "correct_text" not in r:
                    raise ValueError("hint examples need correct_text so leakage can be checked")
                inp["_correct_text"] = r["correct_text"]
            errs = validators.validate_output(r["task"], inp, r["output"])
            if r["language"] not in prompts.LANGS:
                errs.append("language must be en or hi")
            for k in ("source", "licence", "reviewed_by"):
                if not r.get(k):
                    errs.append("missing %s" % k)
            key = json.dumps([r["task"], r["input"]], sort_keys=True, ensure_ascii=False)
            if key in seen:
                errs.append("duplicate input")
            seen.add(key)
            tasks[r["task"]] += 1; langs[r["language"]] += 1
        except Exception as e:  # noqa: BLE001 - report every bad line
            errs = ["unreadable: %s" % e]
        if errs:
            bad.append((ln, errs))
    for ln, e in bad[:50]:
        print("line %d: %s" % (ln, "; ".join(e)))
    print("%d lines, %d rejected. tasks=%s languages=%s" % (n, len(bad), dict(tasks), dict(langs)))
    return 1 if bad or n == 0 else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]) if len(sys.argv) == 2 else __doc__)
