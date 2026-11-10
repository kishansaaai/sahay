#!/usr/bin/env python3
"""Build skill graph + question bank JSON from the compact authoring files in content/src/*.txt.

Format (one block per skill):
  @skill <id> | <name> | <prereq ids comma-separated or -> | <difficulty 1-5> | <hours>
  notes: <reference note>
  res: <resource title>; <resource title>
  m1: <misconception description>        (m1..m3, ids become <skill>.m1 ...)
  q: <stem> || <correct option> || <wrong>~m1 || <wrong>~m2 || <wrong>~m3  [@<difficulty>]
Option order is shuffled with a fixed seed so builds are reproducible.
"""
import json, random, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOMAINS = {
    "dsa": ("Data Structures and Algorithms for placements", "DSA"),
    "mleng": ("Machine Learning Engineering", "ML"),
}


def parse(path):
    skills, cur = [], None
    for ln, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@skill"):
            p = [x.strip() for x in line[6:].split("|")]
            assert len(p) == 5, "%s:%d bad @skill line" % (path.name, ln)
            cur = {"id": p[0], "name": p[1], "prereqs": [] if p[2] == "-" else [x.strip() for x in p[2].split(",")],
                   "difficulty": int(p[3]), "hours": float(p[4]), "resources": [], "notes": "", "mis": {}, "qs": []}
            skills.append(cur)
        elif line.startswith("notes:"):
            cur["notes"] = line[6:].strip()
        elif line.startswith("res:"):
            cur["resources"] = [{"title": x.strip()} for x in line[4:].split(";") if x.strip()]
        elif re.match(r"m\d:", line):
            cur["mis"][line[:2]] = line[3:].strip()
        elif line.startswith("q:"):
            cur["qs"].append((ln, line[2:].strip()))
        else:
            raise SystemExit("%s:%d unrecognised line: %s" % (path.name, ln, line[:60]))
    return skills


def build(dom):
    title, tag = DOMAINS[dom]
    skills = parse(ROOT / "content" / "src" / (dom + ".txt"))
    graph = {"schema": "sahay.skillgraph/1", "domain": dom, "title": title, "skills": []}
    bank = {"schema": "sahay.questions/1", "domain": dom, "misconceptions": [], "questions": []}
    rng = random.Random(20261110)
    for s in skills:
        graph["skills"].append({k: s[k] for k in ("id", "name", "prereqs", "difficulty", "hours", "resources", "notes")})
        for k, d in sorted(s["mis"].items()):
            bank["misconceptions"].append({"id": "%s.%s" % (s["id"], k), "skill": s["id"], "description": d})
        for n, (ln, q) in enumerate(s["qs"], 1):
            diff = s["difficulty"]
            m = re.search(r"\s@(\d)$", q)
            if m:
                diff = int(m.group(1)); q = q[:m.start()]
            parts = [x.strip() for x in q.split("||")]
            stem, correct, wrongs = parts[0], parts[1], parts[2:]
            opts = [{"text": correct, "misconception": None}]
            for w in wrongs:
                t, _, mk = w.rpartition("~")
                if not t:
                    raise SystemExit("%s q%d: wrong option needs ~mK tag: %s" % (s["id"], n, w))
                opts.append({"text": t.strip(), "misconception": "%s.%s" % (s["id"], mk.strip())})
            rng.shuffle(opts)
            bank["questions"].append({"id": "%s.q%d" % (s["id"], n), "skill": s["id"], "difficulty": diff,
                                      "stem": stem, "options": opts,
                                      "answer": next(i for i, o in enumerate(opts) if o["misconception"] is None)})
    (ROOT / "content" / "skillgraphs" / (dom + ".json")).write_text(json.dumps(graph, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    (ROOT / "content" / "questions" / (dom + ".json")).write_text(json.dumps(bank, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print("%s: %d skills, %d questions, %d misconceptions" % (dom, len(graph["skills"]), len(bank["questions"]), len(bank["misconceptions"])))


if __name__ == "__main__":
    for d in (sys.argv[1:] or [d for d in DOMAINS if (ROOT / "content" / "src" / (d + ".txt")).exists()]):
        build(d)
