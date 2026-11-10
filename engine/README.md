# Learning engine (reference implementation, Python)

Deterministic implementation of PRD section 9 with no LLM and no Android dependency.
Run the tests: `python -m unittest discover -s engine/tests`

| Module | PRD | Issue |
| --- | --- | --- |
| knowledge.py | 9.1, 9.2 BKT, forgetting, stability | C06 |
| misconceptions.py | 9.3 | C07 |
| diagnostic.py | 6 (C3) | C08 |
| planner.py | 9.5 | C09 |
| calibration.py | 9.4 hint ladder, calibration | C10 |
| recovery.py | 9.6 knapsack recovery | C11 |
| pacing.py | 9.7 | C14 |

Assumed parameters not fixed by the PRD are marked `ASSUMPTION` in the code (initial stability 2 days, expected
response time 45 s, session chunk 30 min, diagnostic stop rules, fatigue weights).

**Kotlin port:** the app needs these classes in a Kotlin module with no Android dependency (PRD section 7). This
Python version is the specification the Kotlin port must match; port `engine/tests` as the Kotlin unit tests.
Not yet done, and not compiled or tested in Kotlin.
