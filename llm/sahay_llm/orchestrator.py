"""Orchestrator (PRD 10.4, issue A08): validate output, retry twice with the exact error, then fall back.
The learner never sees a parse error: [Result.output] is always valid or None (None means ask a simpler question)."""
import json
import time
from dataclasses import dataclass, field

from . import fallbacks, prompts, validators

MAX_RETRIES = 2


@dataclass
class Result:
    task: str
    output: dict | None
    source: str                 # "model", "retry" or "fallback"
    attempts: int
    errors: list = field(default_factory=list)   # validation errors seen, for the log only
    latency_s: float = 0.0


class Stats:
    """Per-task validity log for the report: first-try validity, validity after retry, fallback rate."""

    def __init__(self):
        self.t = {}

    def record(self, r):
        s = self.t.setdefault(r.task, {"calls": 0, "first_try": 0, "after_retry": 0, "fallback": 0, "attempts": 0})
        s["calls"] += 1
        s["attempts"] += r.attempts
        if r.source == "model":
            s["first_try"] += 1
        elif r.source == "retry":
            s["after_retry"] += 1
        else:
            s["fallback"] += 1

    def summary(self):
        out = {}
        for k, s in sorted(self.t.items()):
            n = max(1, s["calls"])
            out[k] = dict(s, validity_before_retry=round(s["first_try"] / n, 4),
                          validity_after_retry=round((s["first_try"] + s["after_retry"]) / n, 4),
                          fallback_rate=round(s["fallback"] / n, 4))
        return out

    def write_jsonl(self, path):
        with open(path, "w", encoding="utf-8") as f:
            for k, v in self.summary().items():
                f.write(json.dumps(dict(task=k, **v), ensure_ascii=False) + "\n")


class Orchestrator:
    """[generate] is any callable (messages, temperature, max_tokens) -> str; see generators.py."""

    def __init__(self, generate, stats=None, max_retries=MAX_RETRIES, clock=time.monotonic):
        self.generate, self.stats, self.max_retries, self.clock = generate, stats or Stats(), max_retries, clock

    def run(self, task, inp, language="en"):
        spec = prompts.load_task(task)
        t0 = self.clock()
        errors, error, previous = [], None, None
        for attempt in range(1, self.max_retries + 2):
            msgs = prompts.build_messages(task, inp, language, error, previous)
            try:
                raw = self.generate(msgs, spec["temperature"], spec["max_tokens"])
            except Exception as e:  # runtime failure (model unloaded, out of memory): same path as a bad reply
                raw, error = "", "generation failed: %s" % e
                errors.append(error)
                previous = ""
                continue
            try:
                out = validators.extract_json(raw)
                errs = validators.validate_output(task, inp, out)
            except ValueError as e:
                errs = [str(e)]
            if not errs:
                r = Result(task, out, "model" if attempt == 1 else "retry", attempt, errors, self.clock() - t0)
                self.stats.record(r)
                return r
            error, previous = "; ".join(errs[:3]), raw
            errors.append(error)
        r = Result(task, fallbacks.FALLBACKS[task](inp, language), "fallback", self.max_retries + 1, errors, self.clock() - t0)
        self.stats.record(r)
        return r
