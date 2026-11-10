#!/usr/bin/env python3
"""Validate skill graphs and question banks (C01, C02-C05 acceptance criteria).

Skill graph: unique ids, prerequisites exist, graph is acyclic, difficulty 1..5, hours > 0,
reference notes 110-225 words (about 150-300 tokens), >= 1 resource, >= MIN_SKILLS skills.
Question bank: >= MIN_Q questions per skill, exactly one correct option, every wrong option maps to a
known misconception of the same skill, no duplicate option text inside a question, no duplicate stems.
"""
import json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIN_SKILLS, MIN_Q = 35, 6


def validate_graph(g):
    e, ids = [], {}
    for s in g["skills"]:
        if s["id"] in ids: e.append("duplicate skill id " + s["id"])
        ids[s["id"]] = s
        if not 1 <= s["difficulty"] <= 5: e.append("%s: difficulty out of 1..5" % s["id"])
        if not s["hours"] > 0: e.append("%s: hours must be > 0" % s["id"])
        if not s["resources"]: e.append("%s: no resources" % s["id"])
        w = len(s["notes"].split())
        if not 110 <= w <= 225: e.append("%s: notes are %d words (need 110-225, about 150-300 tokens)" % (s["id"], w))
    for s in g["skills"]:
        for p in s["prereqs"]:
            if p not in ids: e.append("%s: unknown prerequisite %s" % (s["id"], p))
    state = {}
    def dfs(n, path):
        state[n] = 1
        for p in ids[n]["prereqs"]:
            if p not in ids: continue
            if state.get(p) == 1: e.append("cycle: " + " -> ".join(path + [n, p]))
            elif p not in state: dfs(p, path + [n])
        state[n] = 2
    for n in ids:
        if n not in state: dfs(n, [])
    if len(g["skills"]) < MIN_SKILLS: e.append("only %d skills (need >= %d)" % (len(g["skills"]), MIN_SKILLS))
    return e


def validate_bank(b, g):
    e, mis, per, stems = [], {m["id"]: m for m in b["misconceptions"]}, {}, set()
    skills = {s["id"] for s in g["skills"]}
    for q in b["questions"]:
        per[q["skill"]] = per.get(q["skill"], 0) + 1
        if q["skill"] not in skills: e.append("%s: unknown skill" % q["id"])
        if q["stem"] in stems: e.append("%s: duplicate stem" % q["id"])
        stems.add(q["stem"])
        if sum(1 for o in q["options"] if o["misconception"] is None) != 1: e.append("%s: need exactly one correct option" % q["id"])
        if q["options"][q["answer"]]["misconception"] is not None: e.append("%s: answer index wrong" % q["id"])
        texts = [o["text"].strip().lower() for o in q["options"]]
        if len(set(texts)) != len(texts): e.append("%s: duplicate option text" % q["id"])
        if len(q["options"]) < 3: e.append("%s: fewer than 3 options" % q["id"])
        for o in q["options"]:
            m = o["misconception"]
            if m is not None and (m not in mis or mis[m]["skill"] != q["skill"]):
                e.append("%s: option maps to unknown/foreign misconception %s" % (q["id"], m))
    for s in skills:
        if per.get(s, 0) < MIN_Q: e.append("%s: only %d questions (need >= %d)" % (s, per.get(s, 0), MIN_Q))
    return e


def main():
    errs = []
    for gp in sorted((ROOT / "content" / "skillgraphs").glob("*.json")):
        g = json.loads(gp.read_text(encoding="utf-8"))
        errs += ["%s: %s" % (gp.name, x) for x in validate_graph(g)]
        qp = ROOT / "content" / "questions" / gp.name
        if qp.exists():
            errs += ["questions/%s: %s" % (gp.name, x) for x in validate_bank(json.loads(qp.read_text(encoding="utf-8")), g)]
        else:
            errs.append("missing question bank for " + gp.name)
        print("checked", gp.name)
    if errs:
        print("\n".join(errs)); sys.exit(1)
    print("validate_content OK")


if __name__ == "__main__":
    main()
