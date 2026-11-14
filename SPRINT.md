# Sprint 1 - Phase 1 Open Qualifying (Nov 10 to Nov 22, 2026)

Official deadline: **23 Nov 2026**. Internal submission target: **22 Nov 2026**.

## Milestones (gates)

| Gate | Due | Pass criterion |
| --- | --- | --- |
| Gate 1 - demo app runs (Nov 11) | 2026-11-11 | Official ExecuTorch Llama Android demo runs on a real phone; repo, CI and content schema ready. |
| Gate 2 - streaming chat on phone (Nov 14) | 2026-11-14 | Our INT4 Llama 3.2 1B streams tokens inside our own app; first benchmark numbers exist. |
| Gate 3 - adaptive loop end to end (Nov 17) | 2026-11-17 | Intake, diagnostic, roadmap, a wrong answer and an adapted plan work end to end, offline. |
| Gate 4 - feature status review (Nov 19) | 2026-11-19 | Every innovation feature is marked Shipped, Prototype or Cut; beta testers have the APK. |
| Gate 5 - feature freeze (Nov 21) | 2026-11-21 | Feature freeze. From here only bug fixes, benchmarks, video and report. |
| Submit - Nov 22 (deadline Nov 23) | 2026-11-22 | Repo, APK, demo video and technical report submitted through the organisers' form. |

## Load per person (story points)

| Person | Stream | Points | Issues |
| --- | --- | --- | --- |
| Sanjith | A - Runtime and ML | 67 | 19 |
| Pradhyut | B - Android and Device | 67 | 22 |
| You (team leader) | C - Learning Engine and Evaluation | 66 | 17 |

## Sanjith: Stream A - Runtime and ML

| ID | Task | Gate | Pts | Pri | Blocked by |
| --- | --- | --- | --- | --- | --- |
| A01 | Run the official ExecuTorch Llama Android demo on a real phone | Gate 1 - demo app runs | 3 | P0 | - |
| A02 | Accept the Llama licence; record licences and credits | Gate 1 - demo app runs | 1 | P0 | - |
| A03 | Export Llama 3.2 1B Instruct to INT4 .pte with a reproducible script | Gate 2 - streaming chat on phone | 5 | P0 | A01, A02 |
| A04 | JNI wrapper: load, streaming generate, stop, unload | Gate 2 - streaming chat on phone | 5 | P0 | A03 |
| A05 | Model manager: lazy load, warm-up, low-memory handling | Gate 2 - streaming chat on phone | 3 | P0 | A04 |
| A06 | Benchmark harness v1: time to first token, tokens/sec, RAM | Gate 2 - streaming chat on phone | 3 | P0 | A04 |
| A07 | Prompt templates and JSON schemas for every LLM task | Gate 3 - adaptive loop end to end | 5 | P0 | A03 |
| A08 | Orchestrator: validate output, retry twice, fall back to templates | Gate 3 - adaptive loop end to end | 5 | P0 | A04, A07 |
| A09 | Grounded explanations with analogy and language parameters (I5) | Gate 3 - adaptive loop end to end | 3 | P1 | A07, A08 |
| A10 | Hint generation without answer leakage (I1, LLM side) | Gate 3 - adaptive loop end to end | 3 | P1 | A07, A08 |
| A11 | Teach-back grading: rubric prompt and validator (I7) | Gate 4 - feature status review | 3 | P1 | A08 |
| A12 | Syllabus compiler: OCR text to skill graph draft with validator (I4) | Gate 4 - feature status review | 3 | P1 | A08 |
| A13 | Thermal, battery and memory governor with four modes (I2) | Gate 4 - feature status review | 5 | P1 | A05 |
| A14 | Sustained 20-minute thermal test with and without the governor | Gate 5 - feature freeze | 3 | P1 | A13 |
| A15 | Energy-per-token and battery-per-session measurement protocol | Gate 5 - feature freeze | 3 | P1 | A06 |
| A16 | LLM evaluation harness: JSON validity, rubric scoring, base vs tuned | Gate 5 - feature freeze | 5 | P1 | A08 |
| A17 | Prompt prefix or KV-cache reuse experiment (X4) | Gate 5 - feature freeze | 3 | P2 | A06 |
| A18 | Offline LoRA fine-tune for tutoring tone and JSON discipline (X5) | Gate 5 - feature freeze | 5 | P2 | A16 |
| A19 | Model checksums and release asset (no model in Git) | Gate 5 - feature freeze | 1 | P0 | A03 |

## Pradhyut: Stream B - Android and Device

| ID | Task | Gate | Pts | Pri | Blocked by |
| --- | --- | --- | --- | --- | --- |
| B01 | Repo skeleton: Gradle modules (app, engine), CI, formatting | Gate 1 - demo app runs | 3 | P0 | - |
| B02 | Release manifest without INTERNET permission, enforced in CI (B4) | Gate 1 - demo app runs | 1 | P0 | B01 |
| B03 | Compose app shell: navigation, theme, string resources | Gate 2 - streaming chat on phone | 3 | P0 | B01 |
| B04 | Room database schema and repositories | Gate 2 - streaming chat on phone | 3 | P0 | B01 |
| B05 | Intake chat UI with quick-reply chips (C1) | Gate 3 - adaptive loop end to end | 5 | P0 | B03, B04, A08 |
| B06 | Streaming tutor chat UI with stop button (C6) | Gate 3 - adaptive loop end to end | 3 | P0 | A04, B03 |
| B07 | Diagnostic quiz UI with confidence slider and hint ladder button (C3, I1) | Gate 3 - adaptive loop end to end | 5 | P0 | B03, B04 |
| B08 | Roadmap screen with reasons and 'why this' sheet | Gate 3 - adaptive loop end to end | 3 | P0 | B03 |
| B09 | Today and study/practice screens | Gate 3 - adaptive loop end to end | 3 | P0 | B03, A09 |
| B10 | Dashboard: mastery heatmap, forecast to goal, calibration chart (C7) | Gate 4 - feature status review | 5 | P0 | B04, C06 |
| B11 | Recover screen: before/after plan and dropped items (I6) | Gate 4 - feature status review | 2 | P1 | C11 |
| B12 | Camera OCR scan for syllabus and textbook page (I4, I8) | Gate 4 - feature status review | 5 | P1 | B03, A12 |
| B13 | Device and energy screen; settings with data export and delete | Gate 4 - feature status review | 3 | P1 | A13 |
| B14 | Thread sweep and INT8 / Llama 3.2 3B comparison runs on device | Gate 4 - feature status review | 3 | P1 | A06 |
| B15 | Beta programme: distribute APK, collect the device table, Hindi review | Gate 4 - feature status review | 2 | P1 | B16 |
| B16 | Debug APK build for testers and daily integration build | Gate 4 - feature status review | 2 | P0 | B01 |
| B17 | QR plan export/import and teacher class heatmap (X3) | Gate 5 - feature freeze | 3 | P2 | B04 |
| B18 | Offline voice: speech to text and text to speech prototype (X2) | Gate 5 - feature freeze | 3 | P2 | B06 |
| B19 | UI localisation: Hindi and English complete, UI strings for 4 more languages | Gate 5 - feature freeze | 3 | P1 | B03 |
| B20 | Accessibility pass: TalkBack, large text, contrast | Gate 5 - feature freeze | 2 | P1 | B03 |
| B21 | Release build, signing, APK on GitHub releases, clean-install test | Gate 5 - feature freeze | 2 | P0 | B02 |
| B22 | Demo video: script, recording, captions in English and Hindi | Submit - Nov 22 | 3 | P0 | B21 |

## You (team leader): Stream C - Learning Engine and Evaluation

| ID | Task | Gate | Pts | Pri | Blocked by |
| --- | --- | --- | --- | --- | --- |
| C01 | Choose two domains; skill graph schema and validator (C2) | Gate 1 - demo app runs | 3 | P0 | - |
| C02 | Skill graph content for domain 1 (at least 35 skills) | Gate 2 - streaming chat on phone | 5 | P0 | C01 |
| C03 | Skill graph content for domain 2 (at least 35 skills) | Gate 3 - adaptive loop end to end | 5 | P0 | C01 |
| C04 | Misconception-tagged question bank for domain 1 (at least 6 per skill) | Gate 3 - adaptive loop end to end | 5 | P0 | C02 |
| C05 | Misconception-tagged question bank for domain 2 | Gate 3 - adaptive loop end to end | 5 | P0 | C03 |
| C06 | Knowledge tracing with forgetting curve and unit tests (C4) | Gate 3 - adaptive loop end to end | 5 | P0 | B01 |
| C07 | Misconception engine: evidence, repair items, resolution | Gate 3 - adaptive loop end to end | 3 | P0 | C06 |
| C08 | Adaptive diagnostic question selection | Gate 3 - adaptive loop end to end | 3 | P0 | C04, C06 |
| C09 | Planner and re-planner (C5) | Gate 3 - adaptive loop end to end | 5 | P0 | C06, C07 |
| C10 | Hint ladder and confidence calibration logic (I1) | Gate 4 - feature status review | 3 | P1 | C06 |
| C11 | Missed-day recovery with knapsack replanning (I6) | Gate 4 - feature status review | 3 | P1 | C09 |
| C12 | Learner simulator with 8 learner types (I3) | Gate 4 - feature status review | 5 | P1 | C09 |
| C13 | Simulator baselines and ablations | Gate 4 - feature status review | 3 | P1 | C12 |
| C14 | Cognitive-load-aware pacing (X1) | Gate 5 - feature freeze | 2 | P2 | C10 |
| C15 | Run simulator experiments and produce report tables and charts | Gate 5 - feature freeze | 3 | P1 | C13 |
| C16 | Technical report: draft, figures, results, limitations | Gate 5 - feature freeze | 5 | P0 | A16, C15 |
| C17 | Content and report review with the faculty mentor | Gate 5 - feature freeze | 3 | P1 | C04 |

## Cut order if time runs short (cut from the top first)

1. A18 offline LoRA  2. B18 voice  3. A17 prefix/KV-cache  4. B17 QR classroom  5. B12 scan features
6. C14 cognitive-load pacing  7. A11 teach-back  8. A12 syllabus compiler  9. A13 governor (keep measurement panel)

Never cut: B1-B4 base layer, C1-C7 learning core, hint ladder, simulator, recover mode, analogies, benchmarks, demo video, report.

## Stream A status (Sanjith) - updated 2026-11-11

"Code done" means written, unit-tested here and in CI. "Needs device/model" is what still has to be run on a phone or with the weights, so the story is **not** closable until then.

| ID | Status | Done in repo | Still needed |
| --- | --- | --- | --- |
| A01 | In progress | `docs/setup-runtime.md` steps and result template | Run the demo on the phone, fill the doc, email blockers |
| A02 | In progress | `LICENSES/` tables and `LLAMA-3.2-NOTICE.md` checklist | Confirm model access (name, date), add the licence text from the download |
| A03 | Code done | `export/build_pte.py`, `prompts20.json`, `releases/CHECKSUMS.txt` | Pin ExecuTorch tag, confirm flag names, build the .pte, 20 prompts on the phone |
| A04 | Code done | `LlmRuntime`, `ExecuTorchRuntime` (opt-in), `SahayLlm.stream` Flow | Compile against the pinned tag; check stop under 500 ms and 200 runs memory on device |
| A05 | Code done | `ModelManager` lazy load, warm-up, `onTrimMemory`, tests | Simulated trim on device |
| A06 | Code done | `BenchStats`, debug `BenchmarkActivity`, `bench/README.md` | Run on the phone, commit numbers |
| A07 | Done | `content/prompts/*`, tests, Python/Kotlin parity in CI | - |
| A08 | Done | Python and Kotlin orchestrators, validity log, tests | Report numbers need the model |
| A09 | Code done | `tutor.explain` | Model-run quality check (A16) |
| A10 | Done | `tutor.hint`, leak validator and tests | - |
| A11 | Code done | rubric prompt, validator, agreement script | 70% agreement on 40 samples needs a human grader |
| A12 | Code done | compiler, validator (acyclic, at most 60), OCR sanitising | 80% topic coverage on 5 real syllabi |
| A13 | Code done | `Governor`, `DeviceMonitor`, tests | Thresholds tuned from the thermal test; debug panel UI (Stream B) |
| A14 | Tooling done | sustained run mode, `bench/thermal_chart.py` | The 20-minute runs |
| A15 | Tooling done | `bench/ENERGY.md`, `energy_calc.py` | The measurements |
| A16 | Tooling done | `eval/` fixed sets (500 per task), rubric scripts, compare | Run with the real model; human reviewers |
| A17 | Documented | `bench/PREFIX_REUSE.md` records the API facts (prefill exists, no partial rollback, so no reuse for divergent prompts) and the shorter-prompt fallback | Send the mentor email; shorter-system-prompt measurement on the phone |
| A18 | Not started | `export/finetune/` validator and training script | Reviewed licensed data, Colab run, evaluation |
| A19 | Code done | `ModelVerifier`, checksum asset bundling, `scripts/verify_model.py`, `releases/README.md` | Attach the model to a GitHub release |

## Stream B status (Pradhyut) - updated 2026-11-14

"Code done" means written and compile-checked by CI. No screen has been rendered and nothing has run on a phone in this environment, so UI rows stay "Code done" until a device test happens.

| ID | Status | Done in repo | Still needed |
| --- | --- | --- | --- |
| B03 | Code done | `SahayApp` shell: three tabs, back handling, runtime language switch via a locale-overridden `LocalContext`, `SahayApplication` owner, theme | Render on a phone; confirm rotation keeps state |
| B04 | Code done | `engine/Store.kt` snapshot model, attempt replay and JSON import/export with tests; `app/data/SnapshotStore.kt` atomic file store and one-tap delete | Render with a screen, device test of save/load across process death |
| B05 | Code done | `IntakeScreen`: chat-style stepper, free-text goal mapped to a domain with a chip fallback, level/hours/deadline/interests, review then create profile | Render on a phone; Hindi copy review |
| B06 | Code done | `TutorScreen`: streaming tokens, Stop, source note, missing-model notice; template reply when no `.pte` | Device run with the model; check Stop under 500 ms |
| B07 | Code done | `DiagnosticScreen`: confidence slider, 3-level hint ladder with template fallback, misconception note, attempt rows with confidence/time/hint level | Device run; measure question count and minutes against the C08 target |
| B08 | Code done | `RoadmapScreen`: week cards, four item types, plan change note, dropped items, "why this" dialog with reason/prereqs/note | Device render; check long roadmaps scroll |
| B09 | Code done | `TodayScreen` and `StudyScreen`: next action with minutes, mastery and streak, explanation + analogy, check question that updates mastery and re-plans | Device run; model-quality check of explanations |
