# LLM layer (PRD 10; issues A07 to A12)

Pure-Python reference of the app's LLM layer, mirrored 1:1 in Kotlin (`engine/src/main/kotlin/com/sahay/engine/LlmOrchestrator.kt`); CI diffs the
prompts both produce (`llm/parity_python.py` vs `LlmParityKt`). Prompts and schemas live in `content/prompts/`.

| Module | Role |
| --- | --- |
| `prompts.py` | Builds the Llama-3 chat prompt: shared system prompt, task instruction, schema, two examples in the output language, input as `<data>` |
| `schema.py`, `validators.py` | Schema check plus the semantic rules (item ids match the plan, no answer leak at hint levels 0 and 1, one correct option, acyclic graph of at most 60 skills, ...) |
| `orchestrator.py` | Validate, retry twice with the exact error appended, then a deterministic fallback; per-task validity log (A08) |
| `fallbacks.py` | Template text, hint ladder, quick-reply request for goals, heuristic teach-back grader |
| `tutor.py` | `explain` (A09), `hint` (A10), `grade_teachback` (A11), `compile_syllabus` and `sanitize_ocr` (A12) |
| `generators.py` | `CommandGenerator` (plug in the exported model on a laptop or phone), `ScriptedGenerator` (tests) |

Design rules kept: Llama never decides plan order, mastery or grades without a deterministic check (`teachback_mastery_evidence` gives
model gradings half weight and the heuristic fallback none); the learner never sees a parse error (`Result.output` is valid or `None`).

    python -m unittest discover -s llm/tests -t .

What is **not** measured yet: validity rates, the 70% teach-back agreement and the 80% syllabus coverage all need the real model and
human graders. Use `eval/` for that (`eval/README.md`).
