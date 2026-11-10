"""Task validators (PRD 10.3): schema check plus the semantic rules each task needs. Return a list of errors."""
import re
from . import prompts, schema


def extract_json(text):
    """First balanced JSON object in [text] (models add code fences or chatter). Returns the parsed value or raises ValueError."""
    import json
    start = text.find("{")
    if start < 0:
        raise ValueError("no JSON object found in the reply")
    depth, in_str, esc = 0, False, False
    for i in range(start, len(text)):
        c = text[i]
        if in_str:
            esc = (c == "\\" and not esc)
            if c == '"' and not esc:
                in_str = False
            continue
        if c == '"':
            in_str = True
        elif c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                try:
                    return json.loads(text[start:i + 1])
                except ValueError as e:
                    raise ValueError("invalid JSON: %s" % e)
    raise ValueError("JSON object is not closed (reply was cut off)")


def norm(s):
    return re.sub(r"\s+", " ", s.strip().lower())


def contains_text(haystack, needle):
    """True if [needle] appears in [haystack] as a whole token sequence (so '1' does not match '10')."""
    n = norm(needle)
    if not n:
        return False
    return re.search(r"(?<![\w.])" + re.escape(n) + r"(?![\w])", norm(haystack)) is not None


def words(s):
    return len(s.split())


def _goal(inp, out):
    return [] if out["domain_id"] in inp.get("domains", []) else ["domain_id '%s' is not one of %s" % (out["domain_id"], inp.get("domains"))]


def _plan(inp, out):
    want = [i["id"] for i in inp["items"]]
    got = [i["id"] for i in out["items"]]
    if got != want:
        return ["items must be exactly %s in this order, got %s" % (want, got)]
    return []


def _explain(inp, out):
    e = []
    if out["skill_id"] != inp["skill_id"]:
        e.append("skill_id must be '%s'" % inp["skill_id"])
    name = inp.get("skill_name", "")
    tail = inp["skill_id"].split(".")[-1].replace("_", " ")
    if not (contains_text(out["explanation"], name) or contains_text(out["explanation"], tail)):
        e.append("explanation must mention the skill '%s'" % name)
    if words(out["explanation"]) > 130:
        e.append("explanation is too long (max 120 words)")
    if inp.get("analogy") and not out["analogy"].strip():
        e.append("analogy is required because analogy is true")
    if not inp.get("analogy") and out["analogy"].strip():
        e.append("analogy must be an empty string because analogy is false")
    return e


def _hint(inp, out):
    e = []
    level = inp["level"]
    correct = inp.get("_correct_text")
    if level not in (0, 1, 2, 3):
        return ["level must be 0 to 3"]
    if correct is not None:
        leaked = contains_text(out["hint"], correct)
        if level <= 1 and leaked:
            e.append("hint must not contain the correct option text at level %d" % level)
        if level == 3 and not leaked:
            e.append("level 3 must state the correct answer")
    if level <= 1 and re.search(r"\b(the )?(correct )?answer is\b|सही उत्तर", out["hint"], re.I):
        e.append("hint must not announce the answer at level %d" % level)
    return e


def _question(inp, out):
    e = []
    texts = [norm(o["text"]) for o in out["options"]]
    if len(set(texts)) != len(texts):
        e.append("options must be unique")
    nulls = [i for i, o in enumerate(out["options"]) if o["misconception"] is None]
    if nulls != [out["answer"]]:
        e.append("exactly one option (the one at index answer) must have misconception null; every wrong option needs a misconception")
    return e


def _teach(inp, out):
    e = []
    if out["skill_id"] != inp["skill_id"]:
        e.append("skill_id must be '%s'" % inp["skill_id"])
    sentences = [s for s in re.split(r"[.!?।]+", out["feedback"]) if s.strip()]
    if len(sentences) > 3:
        e.append("feedback must be two sentences")
    return e


_ID = re.compile(r"^[a-z][a-z0-9_]*$")


def check_graph(compiled, max_skills=60):
    """Errors for a compiled syllabus: ids, prerequisite references, size and acyclicity."""
    e = []
    ids = [s["id"] for s in compiled["skills"]]
    if len(ids) > max_skills:
        e.append("at most %d skills allowed, got %d" % (max_skills, len(ids)))
    if len(set(ids)) != len(ids):
        e.append("skill ids must be unique")
    for i in ids:
        if not _ID.match(i):
            e.append("id '%s' must be lowercase snake_case" % i)
    deps = {i: set() for i in ids}
    for p in compiled["prerequisites"]:
        a, b = p["skill"], p["requires"]
        if a not in deps or b not in deps:
            e.append("prerequisite %s -> %s uses an unknown skill id" % (a, b))
        elif a == b:
            e.append("skill '%s' cannot require itself" % a)
        else:
            deps[a].add(b)
    state = {}
    def dfs(n):
        state[n] = 1
        for m in deps[n]:
            if state.get(m) == 1:
                return True
            if m not in state and dfs(m):
                return True
        state[n] = 2
        return False
    if not e and any(n not in state and dfs(n) for n in deps):
        e.append("prerequisites contain a cycle; the graph must be acyclic")
    return e


def _syllabus(inp, out):
    return check_graph(out)


SEMANTIC = {"goal_parse": _goal, "plan_narrate": _plan, "explain": _explain, "hint": _hint,
            "draft_question": _question, "teachback_grade": _teach, "syllabus_compile": _syllabus}


def validate_output(task, inp, out):
    """Schema check, then the semantic validator. Empty list means the output may be shown to the learner."""
    errs = schema.validate(out, prompts.load_task(task)["schema"])
    return errs if errs else SEMANTIC[task](inp, out)
