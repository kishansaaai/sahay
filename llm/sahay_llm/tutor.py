"""Task helpers that sit on the orchestrator: explanations (A09), hints (A10), teach-back (A11), syllabus compiler (A12)."""
import json
import re
from pathlib import Path

from . import validators

CONTENT_ROOT = Path(__file__).resolve().parents[2] / "content"
HINT_LEVELS = ["nudge", "hint", "worked_step", "solution"]


def skill_index(root=CONTENT_ROOT):
    """skill id -> {name, notes, domain} from every skill graph the app bundles."""
    idx = {}
    for p in sorted((Path(root) / "skillgraphs").glob("*.json")):
        g = json.loads(p.read_text(encoding="utf-8"))
        for s in g["skills"]:
            idx[s["id"]] = {"name": s["name"], "notes": s["notes"], "domain": g["domain"]}
    return idx


def explain(orch, index, skill_id, question, language="en", interests=(), analogy=False):
    """Grounded explanation: the skill's reference note is looked up by id and pasted into the prompt (PRD 10.2).
    Returns (Result, source_note); the note backs the 'show source note' button."""
    s = index[skill_id]
    inp = {"skill_id": skill_id, "skill_name": s["name"], "notes": s["notes"], "question": question,
           "interests": list(interests), "analogy": bool(analogy and interests), "language": language}
    return orch.run("explain", inp, language), s["notes"]


def hint(orch, question, learner_answer, level, language="en", notes=""):
    """One rung of the hint ladder. [question] is a content question dict (stem, options, answer).
    The correct option is passed to the validator privately and never put in the prompt."""
    if level not in (0, 1, 2, 3):
        raise ValueError("level must be 0, 1, 2 or 3")
    opts = [o["text"] for o in question["options"]]
    inp = {"stem": question["stem"], "options": opts, "learner_answer": learner_answer, "level": level, "notes": notes,
           "_correct_text": opts[question["answer"]]}
    return orch.run("hint", inp, language)


def grade_teachback(orch, index, skill_id, learner_text, language="en"):
    s = index[skill_id]
    return orch.run("teachback_grade", {"skill_id": skill_id, "notes": s["notes"], "learner_text": learner_text}, language)


def teachback_mastery_evidence(result):
    """Mastery evidence in 0..1 from a grading. Model gradings count at half weight; the heuristic fallback does not
    count at all, so the LLM alone never moves the plan (PRD 10 preamble)."""
    if result.output is None or result.source == "fallback":
        return None
    sc = result.output["scores"]
    return 0.5 * (sc["accuracy"] + sc["completeness"] + sc["clarity"] + sc["example"]) / 12.0


def agreement(model_scores, human_scores, tolerance=0):
    """Share of gradings where every rubric dimension is within [tolerance] points of the human grader (A11 target 0.70)."""
    if len(model_scores) != len(human_scores) or not model_scores:
        raise ValueError("need the same non-zero number of gradings")
    ok = sum(all(abs(m[k] - h[k]) <= tolerance for k in h) for m, h in zip(model_scores, human_scores))
    return ok / len(model_scores)


_INJECTION = re.compile(r"(ignore|disregard|forget)\s+(all\s+)?(the\s+)?(previous|above|prior)\s+(instructions|prompts?)|system prompt", re.I)


def sanitize_ocr(text, max_chars=6000):
    """OCR text is data: strip control characters, cap length, defuse delimiter tags and drop obvious injection lines."""
    text = re.sub(r"[\x00-\x08\x0b-\x1f\x7f]", " ", text)
    text = re.sub(r"</?\s*data\s*>", " ", text, flags=re.I)
    lines = [l for l in text.splitlines() if not _INJECTION.search(l)]
    return "\n".join(lines)[:max_chars]


def compile_syllabus(orch, ocr_text, language="en"):
    """OCR text to a skill graph draft. Invalid (cyclic, over-60, dangling) graphs never leave this function."""
    return orch.run("syllabus_compile", {"ocr_text": sanitize_ocr(ocr_text)}, language)


def to_skill_graph(compiled, domain, title):
    """Convert a validated compile result to the sahay.skillgraph/1 shape (notes to be filled in by the author or by the explain task)."""
    errs = validators.check_graph(compiled)
    if errs:
        raise ValueError("; ".join(errs))
    pre = {s["id"]: [] for s in compiled["skills"]}
    for p in compiled["prerequisites"]:
        pre[p["skill"]].append(p["requires"])
    return {"schema": "sahay.skillgraph/1", "domain": domain, "title": title,
            "skills": [{"id": "%s.%s" % (domain, s["id"]), "name": s["name"], "prereqs": ["%s.%s" % (domain, r) for r in pre[s["id"]]],
                        "difficulty": 2, "hours": float(s["hours"]), "resources": [], "notes": ""} for s in compiled["skills"]]}


def topic_coverage(compiled, expected_topics):
    """Share of [expected_topics] (strings) matched by a compiled skill name; used for the 80% target on real syllabi."""
    names = [validators.norm(s["name"]) for s in compiled["skills"]]
    hit = sum(any(validators.norm(t) in n or n in validators.norm(t) for n in names) for t in expected_topics)
    return hit / max(1, len(expected_topics))
