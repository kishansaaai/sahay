"""Skill graph and question bank loading (PRD section 8). Same JSON files the app bundles."""
import json
from pathlib import Path

CONTENT_ROOT = Path(__file__).resolve().parents[2] / "content"


class Graph:
    def __init__(self, skills):
        self.skills = {s["id"]: s for s in skills}
        self._dependents = {k: [] for k in self.skills}
        for s in skills:
            for p in s["prereqs"]:
                self._dependents[p].append(s["id"])
        self._clo = {}

    def prereqs(self, sid):
        return self.skills[sid]["prereqs"]

    def dependents(self, sid):
        return self._dependents[sid]

    def prereq_closure(self, sid):
        """All transitive prerequisites of sid (excluding sid)."""
        if sid not in self._clo:
            out = set()
            for p in self.prereqs(sid):
                out.add(p)
                out |= self.prereq_closure(p)
            self._clo[sid] = frozenset(out)
        return self._clo[sid]

    def closure(self, targets):
        """Targets plus every prerequisite of each target."""
        out = set()
        for t in targets:
            out.add(t)
            out |= self.prereq_closure(t)
        return out

    def topo_order(self, ids=None):
        ids = set(self.skills) if ids is None else set(ids)
        indeg = {i: sum(1 for p in self.prereqs(i) if p in ids) for i in ids}
        ready = sorted(i for i, d in indeg.items() if d == 0)
        out = []
        while ready:
            n = ready.pop(0)
            out.append(n)
            for d in sorted(self.dependents(n)):
                if d in ids:
                    indeg[d] -= 1
                    if indeg[d] == 0:
                        ready.append(d)
            ready.sort()
        if len(out) != len(ids):
            raise ValueError("skill graph has a cycle")
        return out

    def est_hours(self, sid):
        return self.skills[sid]["hours"]


class Content:
    def __init__(self, graph_json, bank_json):
        self.domain = graph_json["domain"]
        self.graph = Graph(graph_json["skills"])
        self.misconceptions = {m["id"]: m for m in bank_json["misconceptions"]}
        self.questions = {q["id"]: q for q in bank_json["questions"]}
        self.by_skill = {}
        for q in bank_json["questions"]:
            self.by_skill.setdefault(q["skill"], []).append(q)

    @classmethod
    def load(cls, domain, root=None):
        root = Path(root) if root else CONTENT_ROOT
        g = json.loads((root / "skillgraphs" / (domain + ".json")).read_text(encoding="utf-8"))
        b = json.loads((root / "questions" / (domain + ".json")).read_text(encoding="utf-8"))
        return cls(g, b)

    def misconceptions_of(self, skill):
        return [m for m in self.misconceptions.values() if m["skill"] == skill]
