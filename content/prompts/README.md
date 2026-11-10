# Prompts (issue A07, PRD 10.2 and 10.3)

| File | Task |
| --- | --- |
| `system.txt` | The one system prompt, identical for every task so a shared prefix can be cached (X4). Under 300 tokens (CI checks). |
| `goal_parse.json` | Free-text goal to goal, domain id, level, weeks |
| `plan_narrate.json` | Narrate a deterministic plan; item ids must match |
| `explain.json` | Grounded explanation with optional interest-based analogy and one check question |
| `hint.json` | One hint-ladder rung (nudge, hint, worked step, solution) |
| `draft_question.json` | One multiple-choice question with misconception per wrong option |
| `teachback_grade.json` | Rubric scores 0 to 3 for accuracy, completeness, clarity, example, plus feedback |
| `syllabus_compile.json` | Scanned syllabus text to skills and prerequisites (acyclic, at most 60) |

Each task file has `instruction` in English and Hindi, the JSON `schema`, `temperature`, `max_tokens` and four `examples` (two English, two Hindi).
A prompt shows only the two examples in the requested language. Inputs go inside `<data>` tags and are never instructions.

Files are generated from the structure the validators and the Kotlin port read; edit the JSON directly, then run:

    python -m unittest discover -s llm/tests -t .       # every example must satisfy its own schema and validator
    python llm/parity_python.py | sha256sum             # prompts must stay identical to the Kotlin port (CI diffs them)

Changing a template changes `eval/MANIFEST.json` only if the fixed prompt sets change; re-run the evaluation for any template change.
