"""Deterministic fallbacks (PRD 10.4 step 3). Each takes the task input and returns a valid output or None.
None means the caller must ask a simpler question (goal parsing) or skip the optional feature (question drafting)."""
import re

HINT_FALLBACK = {
    "en": ["Re-read the question slowly and say in your own words what it is asking.",
           "Look at the key idea in your notes for this topic and test each option against it.",
           "Work the first step on paper, then rule out the options that break that step.",
           "The correct answer is: %s."],
    "hi": ["प्रश्न को धीरे से दोबारा पढ़ें और अपने शब्दों में बताएँ कि वह क्या पूछ रहा है।",
           "इस विषय के नोट्स का मुख्य विचार देखें और हर विकल्प को उससे जाँचें।",
           "पहला चरण कागज़ पर हल करें, फिर जो विकल्प उसे तोड़ते हैं उन्हें हटाएँ।",
           "सही उत्तर है: %s।"],
}
CHECK_Q = {"en": "In one sentence, how would you explain %s to a friend?", "hi": "एक वाक्य में बताएँ कि आप %s अपने मित्र को कैसे समझाएँगे?"}
_PLAN = {"en": "%d min: %s (%s).", "hi": "%d मिनट: %s (%s)।"}
_SUMMARY = {"en": "Here is your plan, one step at a time.", "hi": "यह रही आपकी योजना, एक-एक कदम करके।"}


def goal_parse(inp, lang):
    text = inp["goal"].lower()
    hits = [d for d in inp.get("domains", []) if d.lower() in text]
    if len(hits) != 1:
        return None
    m = re.search(r"(\d+)\s*(week|wk|हफ्ते|सप्ताह|month|महीने)", text)
    weeks = 8
    if m:
        weeks = int(m.group(1)) * (4 if m.group(2) in ("month", "महीने") else 1)
    return {"goal": inp["goal"][:200], "domain_id": hits[0], "target_level": "intermediate", "timeframe_weeks": max(1, min(52, weeks))}


def plan_narrate(inp, lang):
    return {"summary": _SUMMARY[lang],
            "items": [{"id": i["id"], "text": (_PLAN[lang] % (i["minutes"], i["skill"], i["kind"]))[:160]} for i in inp["items"]]}


def explain(inp, lang):
    sentences = [s.strip() for s in re.split(r"(?<=[.।])\s+", inp.get("notes", "")) if s.strip()][:3]
    name = inp.get("skill_name") or inp["skill_id"].split(".")[-1]
    text = ("%s: %s" % (name, " ".join(sentences))).strip()
    return {"skill_id": inp["skill_id"], "explanation": text[:900], "analogy": "", "check_question": CHECK_Q[lang] % name}


def hint(inp, lang):
    level = inp["level"]
    if level == 3:
        correct = inp.get("_correct_text")
        text = HINT_FALLBACK[lang][3] % correct if correct is not None else HINT_FALLBACK[lang][2]
    else:
        text = HINT_FALLBACK[lang][level]
    return {"hint": text}


def draft_question(inp, lang):
    return None


_EX = re.compile(r"\b(for example|for instance|such as|like)\b|उदाहरण|जैसे", re.I)


def teachback_grade(inp, lang):
    """Heuristic grader used only when the model fails; its feedback says so and it never feeds mastery on its own."""
    notes = {w for w in re.findall(r"\w{5,}", inp.get("notes", "").lower())}
    got = {w for w in re.findall(r"\w{5,}", inp["learner_text"].lower())}
    cover = len(notes & got) / max(1, len(notes))
    n = len(inp["learner_text"].split())
    comp = 3 if cover > 0.4 else 2 if cover > 0.2 else 1 if cover > 0.05 else 0
    clar = 0 if n < 4 else 1 if n < 10 else 2 if n < 80 else 3
    ex = 2 if _EX.search(inp["learner_text"]) else 0
    fb = {"en": "We could not review this in detail. Add what the notes say and one real example.",
          "hi": "हम इसे विस्तार से नहीं जाँच सके। नोट्स की बात और एक वास्तविक उदाहरण जोड़ें।"}[lang]
    return {"task": "teachback_grade", "skill_id": inp["skill_id"], "scores": {"accuracy": comp, "completeness": comp, "clarity": clar, "example": ex}, "feedback": fb}


def syllabus_compile(inp, lang):
    skills, seen = [], set()
    for line in re.split(r"[\n.;]+", inp["ocr_text"]):
        name = re.sub(r"^\s*(unit|chapter|topic|इकाई)?\s*\d*[\).:\-]*\s*", "", line.strip(), flags=re.I)
        name = re.sub(r"\(.*?\)", "", name).strip()
        sid = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
        if not name or not re.match(r"^[a-z]", sid) or sid in seen or len(skills) >= 60:
            continue
        seen.add(sid)
        skills.append({"id": sid[:40], "name": name[:80], "hours": 3})
    return {"skills": skills, "prerequisites": []} if skills else None


FALLBACKS = {"goal_parse": goal_parse, "plan_narrate": plan_narrate, "explain": explain, "hint": hint,
             "draft_question": draft_question, "teachback_grade": teachback_grade, "syllabus_compile": syllabus_compile}
