"""Prompt templates (PRD 10.2): one shared system prompt, per-task instruction, schema and two examples in the output language."""
import json
from pathlib import Path

PROMPT_DIR = Path(__file__).resolve().parents[2] / "content" / "prompts"
TASKS = ["goal_parse", "plan_narrate", "explain", "hint", "draft_question", "teachback_grade", "syllabus_compile"]
LANGS = ("en", "hi")
EXPERIMENTAL_LANGS = {"ta": "Tamil", "te": "Telugu", "bn": "Bengali", "mr": "Marathi"}
SYSTEM_TOKEN_LIMIT = 300

_cache = {}


def estimate_tokens(text):
    """Conservative token estimate for Llama 3 (Devanagari costs several tokens per word)."""
    return int(max(len(text.split()) * 1.5, len(text) / 3.0)) + 1


def system_prompt():
    if "system" not in _cache:
        _cache["system"] = (PROMPT_DIR / "system.txt").read_text(encoding="utf-8").strip()
    return _cache["system"]


def load_task(name):
    if name not in _cache:
        _cache[name] = json.loads((PROMPT_DIR / (name + ".json")).read_text(encoding="utf-8"))
    return _cache[name]


def _data(obj):
    """Wrap input as data. Public keys only (keys starting with '_' are private to validators). Closing tags are neutralised."""
    pub = {k: v for k, v in obj.items() if not k.startswith("_")}
    return "<data>" + json.dumps(pub, ensure_ascii=False, separators=(",", ":")).replace("</data>", "<\\/data>") + "</data>"


def build_messages(task, inp, language="en", error=None, previous=None):
    """Chat messages for [task]. On retry pass the invalid [previous] reply and the exact validation [error]."""
    if language not in LANGS and language not in EXPERIMENTAL_LANGS:
        raise ValueError("language must be one of %s" % (LANGS,))
    t = load_task(task)
    template_language = "hi" if language == "hi" else "en"
    ex = [e for e in t["examples"] if e["lang"] == template_language]
    shown = "\n\n".join("Input: %s\nOutput: %s" % (_data(e["input"]), json.dumps(e["output"], ensure_ascii=False, separators=(",", ":"))) for e in ex)
    user = "%s\nLanguage: %s\nSchema: %s\n\n%s\n\nInput: %s\nOutput:" % (
        t["instruction"][template_language], {"en": "English", "hi": "Hindi", **EXPERIMENTAL_LANGS}[language],
        json.dumps(t["schema"], ensure_ascii=False, separators=(",", ":")), shown, _data(inp))
    msgs = [{"role": "system", "content": system_prompt()}, {"role": "user", "content": user}]
    if error is not None:
        msgs.append({"role": "assistant", "content": previous or ""})
        msgs.append({"role": "user", "content": "That reply was invalid: %s\nReply again with only the corrected JSON object." % error})
    return msgs


def to_llama3(messages):
    """Render messages in the Llama 3 instruct chat format used by the ExecuTorch runner."""
    s = "<|begin_of_text|>"
    for m in messages:
        s += "<|start_header_id|>%s<|end_header_id|>\n\n%s<|eot_id|>" % (m["role"], m["content"])
    return s + "<|start_header_id|>assistant<|end_header_id|>\n\n"
