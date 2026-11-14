"""Fixed prompt sets for the LLM evaluation (PRD 12.3): 500 deterministic prompts per task, built from the bundled content.
Nothing is stored in Git except MANIFEST.json (task -> SHA-256 of the set), so a changed set is detected."""
import hashlib
import json
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from llm.sahay_llm import tutor  # noqa: E402

N = 500
SEED = 20261110
MANIFEST = Path(__file__).with_name("MANIFEST.json")
INTERESTS = {"en": ["cricket", "cooking", "movies", "trains", "music"], "hi": ["क्रिकेट", "खाना बनाना", "फ़िल्में", "रेलगाड़ी", "संगीत"]}
QUESTIONS = {"en": ["Can you explain this simply?", "Why does this matter in interviews?", "What is the key idea?", "How do I remember this?"],
             "hi": ["क्या आप इसे सरल शब्दों में समझा सकते हैं?", "यह क्यों ज़रूरी है?", "मुख्य विचार क्या है?", "इसे याद कैसे रखूँ?"]}
GOALS = {"en": ["I want to prepare {d} in {w} weeks", "Help me learn {d} for placements, I have {w} weeks", "Need {d} basics before my exam in {w} weeks"],
         "hi": ["मुझे {w} हफ्ते में {d} की तैयारी करनी है", "प्लेसमेंट के लिए {d} सीखना है, मेरे पास {w} हफ्ते हैं", "{w} हफ्ते में परीक्षा है, {d} की बुनियाद चाहिए"]}
DOMAIN_WORDS = {"dsa": ("dsa", "डेटा स्ट्रक्चर dsa"), "mleng": ("mleng", "मशीन लर्निंग mleng")}


def _bank():
    qs = []
    for p in sorted((ROOT / "content" / "questions").glob("*.json")):
        qs += json.loads(p.read_text(encoding="utf-8"))["questions"]
    return qs


def build(task, n=N, seed=SEED):
    """List of {task, language, input} for [task]. Deterministic for a given content version."""
    rnd = random.Random("%s/%s" % (seed, task))
    idx = tutor.skill_index()
    ids = sorted(idx)
    qs = _bank()
    out = []
    if task == "explain":
        for b in range(n // 2):
            sid = rnd.choice(ids)
            interests = rnd.sample(INTERESTS["en"], rnd.randint(0, 2))
            analogy = bool(interests) and rnd.random() < 0.5
            qi = rnd.randrange(4)
            for lang in ("en", "hi"):
                ints = [INTERESTS["hi"][INTERESTS["en"].index(i)] for i in interests] if lang == "hi" else interests
                out.append({"task": task, "language": lang, "pair": b, "input": {
                    "skill_id": sid, "skill_name": idx[sid]["name"], "notes": idx[sid]["notes"], "question": QUESTIONS[lang][qi],
                    "interests": ints, "analogy": analogy, "language": lang}})
        return out
    for i in range(n):
        lang = "en" if i % 2 == 0 else "hi"
        if task == "goal_parse":
            dom = rnd.choice(sorted(DOMAIN_WORDS))
            w = rnd.choice([4, 6, 8, 10, 12])
            inp = {"goal": rnd.choice(GOALS[lang]).format(d=DOMAIN_WORDS[dom][lang == "hi"], w=w), "domains": sorted(DOMAIN_WORDS)}
        elif task == "plan_narrate":
            k = rnd.randint(1, 6)
            inp = {"items": [{"id": "p%d" % (j + 1), "kind": rnd.choice(["learn", "repair", "revise"]), "skill": idx[(s := rnd.choice(ids))]["name"], "minutes": rnd.choice([10, 15, 20, 30, 40])} for j in range(k)]}
        elif task == "hint":
            q = rnd.choice(qs)
            wrong = [o["text"] for j, o in enumerate(q["options"]) if j != q["answer"]]
            inp = {"stem": q["stem"], "options": [o["text"] for o in q["options"]], "learner_answer": rnd.choice(wrong), "level": i % 4,
                   "notes": idx[q["skill"]]["notes"][:300], "_correct_text": q["options"][q["answer"]]["text"]}
        elif task == "draft_question":
            sid = rnd.choice(ids)
            inp = {"skill_id": sid, "notes": idx[sid]["notes"]}
        elif task == "teachback_grade":
            sid = rnd.choice(ids)
            words = idx[sid]["notes"].split()
            keep = rnd.choice([6, 15, 40, 90])
            inp = {"skill_id": sid, "notes": idx[sid]["notes"], "learner_text": " ".join(words[:keep])}
        elif task == "syllabus_compile":
            k = rnd.randint(3, 12)
            names = [idx[s]["name"] for s in rnd.sample(ids, k)]
            lines = ["Unit %d %s%s" % (j + 1, n_, " (needs unit %d)" % j if j else "") for j, n_ in enumerate(names)]
            inp = {"ocr_text": "\n".join(lines)}
        else:
            raise ValueError(task)
        out.append({"task": task, "language": lang, "input": inp})
    return out


def digest(items):
    return hashlib.sha256(json.dumps(items, sort_keys=True, ensure_ascii=False).encode("utf-8")).hexdigest()


def manifest():
    from llm.sahay_llm.prompts import TASKS
    return {t: digest(build(t)) for t in TASKS}


if __name__ == "__main__":
    if "--write" in sys.argv:
        MANIFEST.write_text(json.dumps(manifest(), indent=1) + "\n")
        print("wrote", MANIFEST)
    else:
        want, got = json.loads(MANIFEST.read_text()), manifest()
        bad = [t for t in want if want[t] != got[t]]
        print("prompt sets match MANIFEST.json" if not bad else "prompt sets changed: %s (content edited? rerun with --write and note it in the report)" % bad)
        sys.exit(1 if bad else 0)
