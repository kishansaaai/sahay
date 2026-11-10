"""Shard runner. One shard = (condition, learner type, domain) over N learners with fixed seeds. Resumable."""
import json
import multiprocessing as mp
from pathlib import Path

from .learner import TYPE_NAMES
from .run import CONDITIONS, run

ROOT = Path(__file__).resolve().parents[1]
DOMAINS = ("dsa", "mleng")


def shard_path(outdir, name):
    return Path(outdir) / "shards" / (name + ".json")


def _do(job):
    outdir, name, kind, args = job
    p = shard_path(outdir, name)
    if p.exists():
        return name, 0
    rows = []
    if kind == "main":
        cond, ltype, domain, n = args
        rows = [run(domain, ltype, i, cond) for i in range(n)]
    else:
        mode, ltype, domain, n = args
        gap = None if mode == "nogap" else (15, 7, mode)
        for i in range(n):
            r = run(domain, ltype, i, "full", horizon=42, recovery_gap=gap)
            r["cond"] = "gap_" + mode
            rows.append(r)
    p.parent.mkdir(parents=True, exist_ok=True)
    tmp = p.with_suffix(".tmp")
    tmp.write_text(json.dumps(rows), encoding="utf-8")
    tmp.replace(p)
    return name, len(rows)


def jobs(outdir, n_per_type_domain):
    out = []
    for cond in CONDITIONS:
        for lt in TYPE_NAMES:
            for d in DOMAINS:
                out.append((str(outdir), "main_%s_%s_%s" % (cond, lt, d), "main", (cond, lt, d, n_per_type_domain)))
    for mode in ("nogap", "replan", "recover"):
        for d in DOMAINS:
            out.append((str(outdir), "gap_%s_average_%s" % (mode, d), "gap", (mode, "average", d, n_per_type_domain)))
    return out


def run_all(outdir, n_per_type_domain=500, procs=2, budget_s=None):
    import time
    t0 = time.time()
    todo = [j for j in jobs(outdir, n_per_type_domain) if not shard_path(outdir, j[1]).exists()]
    print("%d shards to run" % len(todo), flush=True)
    done = 0
    with mp.Pool(procs) as pool:
        for name, n in pool.imap_unordered(_do, todo):
            done += 1
            print("done %s (%d)  [%d/%d]" % (name, n, done, len(todo)), flush=True)
            if budget_s and time.time() - t0 > budget_s:
                pool.terminate()
                print("time budget reached; rerun the same command to continue", flush=True)
                return False
    return True
