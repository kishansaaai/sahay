#!/usr/bin/env python3
"""Export a reproducible sample of the content for subject-expert / faculty-mentor review (issue C17)."""
import json, random
from pathlib import Path
root = Path(__file__).resolve().parent.parent
out = root / "docs" / "review"
out.mkdir(parents=True, exist_ok=True)
for dom in ("dsa", "mleng"):
    g = json.loads((root / "content" / "skillgraphs" / (dom + ".json")).read_text(encoding="utf-8"))
    b = json.loads((root / "content" / "questions" / (dom + ".json")).read_text(encoding="utf-8"))
    mis = {m["id"]: m["description"] for m in b["misconceptions"]}
    rng = random.Random(1110)
    qs = rng.sample(b["questions"], 40)
    L = ["# Review sample: %s" % g["title"], "",
         "Please mark each question OK / FIX / REMOVE and write corrections in the right-hand column of the sign-off sheet.",
         "40 questions drawn with a fixed seed from %d. The tag after each wrong option is the misconception it is meant to reveal." % len(b["questions"]), ""]
    for n, q in enumerate(qs, 1):
        L.append("**%d. [%s] (difficulty %d)** %s" % (n, q["id"], q["difficulty"], q["stem"]))
        for i, o in enumerate(q["options"]):
            tag = "  <- CORRECT" if o["misconception"] is None else "  <- reveals: %s" % mis[o["misconception"]]
            L.append("   %s) %s%s" % ("ABCD"[i], o["text"], tag))
        L.append("")
    L += ["## Skill reference notes to review (same seed, 8 skills)", ""]
    for s in rng.sample(g["skills"], 8):
        L += ["### %s - %s" % (s["id"], s["name"]), "Prerequisites: %s; difficulty %d; %.1f h" % (", ".join(s["prereqs"]) or "none", s["difficulty"], s["hours"]), "", s["notes"], ""]
    (out / ("sample_%s.md" % dom)).write_text("\n".join(L), encoding="utf-8")
(out / "SIGNOFF.md").write_text("""# Content and report sign-off (issue C17)

Fill this in after the faculty mentor and subject reviewers have reviewed the samples in this folder, and commit it.

| Item | Reviewer name | Date | Result (OK / changes requested) | Notes |
| --- | --- | --- | --- | --- |
| DSA skill graph (35 skills) | | | | |
| DSA question sample (40) | | | | |
| ML engineering skill graph (35 skills) | | | | |
| ML engineering question sample (40) | | | | |
| Technical report (docs/REPORT.md) | | | | |

Questions marked FIX or REMOVE are listed below with the change made.

""", encoding="utf-8")
print("wrote review pack to", out)
