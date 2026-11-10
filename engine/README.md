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

## Kotlin port (issue C18)

`src/main/kotlin/com/sahay/engine/` is a line-for-line port of `sahay_engine/` (pure JVM Kotlin, no dependencies).
`src/test/kotlin/.../EngineTests.kt` ports all the Python tests (27 checks, runner needs no JUnit) and `Parity.kt` prints a
fixed scenario that must match `parity_python.py` line for line (plans, recovery, dropped reasons). Ties are broken by id in
both implementations so the output is deterministic.

    kotlinc engine/src/main/kotlin -include-runtime -d engine.jar
    kotlinc -cp engine.jar engine/src/test/kotlin -d tests
    java -cp engine.jar:tests com.sahay.engine.EngineTestsKt content
    python3 engine/parity_python.py | diff - <(java -cp engine.jar:tests com.sahay.engine.ParityKt content)

Compiled and tested with kotlinc 2.0.21 on the JVM. Not yet built inside the Android Gradle project: `build.gradle.kts` is
provided for Stream B to include as `:engine`.
