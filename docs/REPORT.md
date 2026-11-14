# Sahay: an on-device adaptive learning companion
Technical report, Arm Bharat AI-SoC Challenge 2nd Edition, Problem Statement 4. **DRAFT** (Stream C sections written; sections marked **[TO FILL]** need numbers or text from Streams A and B and from the pilot).

Section numbers follow PRD 17.2. Every simulator number below is reproduced by `python simulator/run_all.py` (seeded, about 20 minutes on a laptop). Engine claims are covered twice: `python -m unittest discover -s engine/tests` for the Python reference and the Kotlin test runner in CI for the code that ships in the APK.

## 1. Problem and users
Learners in Tier-2 and Tier-3 cities study on mid-range Android phones with patchy connectivity and no personal mentor. Generic chatbots ignore a learner's exact gaps and need a network; classroom curricula ignore pace. Sahay builds and re-plans a study roadmap on the phone, with no INTERNET permission. Personas and scenarios: PRD sections 4 and 13. **[TO FILL: one paragraph from the pilot on who actually used it.]**

## 2. System overview and architecture
Four layers and nine components (PRD section 7): Compose UI, a deterministic learning engine, an LLM orchestrator, device services, ExecuTorch runtime (Llama 3.2 1B INT4), thermal governor, local storage and a laptop-side simulator that reuses the engine and the content files. Storage is a dependency-free JSON snapshot store in `engine/Store.kt` (`sahay.snapshot/1`) rather than Room: it needs no annotation processor, it is the same format the export, import and QR share use, and a stored attempt log replays into exactly the state the live session produced because both paths call the same `applyAnswer()`. The app has never been run on a device, so the store has been exercised only by unit tests. **[TO FILL: architecture diagram export from the PRD; Streams A and B to confirm what shipped.]**

## 3. Why a deterministic engine plus on-device Llama
A bare 1B model writes fluent but unreliable study plans: it can order topics against their prerequisites, mis-track mastery and invent facts. The split used here keeps every decision (what to study, in what order, when to revise, which misconception to repair) in plain, unit-tested code, and uses Llama only for language: explanations, hints, analogies and plan wording, always grounded in a skill's reference notes and validated with retry and template fallback (PRD 10.4). In the simulator the planner never produced a prerequisite violation in 8,000 learners (Table 1), while a random order averaged 6.9 violations per learner; a model-ordered baseline is **[TO FILL: needs the on-device model, not run]**.

## 4. Model export, quantization and ExecuTorch integration on Arm
**[TO FILL by Stream A: export recipe, INT4/INT8, XNNPACK, JNI, streaming, stop latency.]**

## 5. Learning engine: knowledge tracing, misconceptions, planner
Implemented as specified in PRD 9, twice: `engine/sahay_engine/` is the Python reference used by the simulator, and `engine/src/main/kotlin/com/sahay/engine/` is the Kotlin implementation that ships in the APK. The Kotlin suite runs 61 checks in CI (39 engine, 22 LLM orchestration) over the same hand-computed reference values, so both implementations are pinned to one set of numbers instead of being compared by inspection. Kotlin is the implementation of record for the app; Python stays because the simulator imports it.

- **Knowledge tracing (C4):** Bayesian knowledge tracing per skill with slip 0.10, learning rate 0.15 and guess set from the option count; answers after hints count with weights 1.0, 0.7, 0.4 and 0 by scaling the update toward the prior. Hand-computed reference values for the update equations are unit tests in both implementations.
- **Forgetting:** effective mastery is p times exp(-dt/S); stability S grows by 1.5 to 2.5 times on a successful review (scaled by confidence and speed) and halves, minimum one day, on a failed one; revision is due when effective mastery falls below 0.7.
- **Misconception engine:** each wrong option maps to a misconception; confident wrong answers weigh twice a guess; two confident hits create a repair item placed before the skills that depend on it; a misconception is resolved after three correct answers on questions where it was a distractor, spread over at least two days; a relapse reopens it.
- **Planner:** target set, need = gap x weight / hours, topological order with highest need first, fill to the declared hours per day, repair before the skill it unblocks, spaced revision, interleaving; it returns a reason string. Over 1,000 random learners and goals every plan was prerequisite-valid, stayed within the daily minutes and deadline, and the slowest plan took far under the 1 second budget (a typical plan about 3 ms).
- **Forecast:** the projected days to target are a least-squares slope over the per-study-day mean mastery of the goal closure. When there are too few days, no upward trend, or the projection runs past the horizon, the function returns no date and names the reason (`ForecastBasis`) instead of printing a number.
- **Content (C2, C3):** two domains, DSA for placements and ML engineering, 35 skills each (70 total), 6 questions per skill (420 total), 3 named misconceptions per skill (210 total); every wrong option maps to one; validated in CI (acyclic graphs, exactly one correct option, no duplicate options).
- **Content (C2, C3):** two domains, DSA for placements and ML engineering, 35 skills each (70 total), 6 questions per skill (420 total), 3 named misconceptions per skill (210 total); every wrong option maps to one; validated in CI (acyclic graphs, exactly one correct option, no duplicate options).

Assumptions the PRD leaves open are marked `ASSUMPTION` in the code: initial stability 2 days, reference response time 45 s, 30-minute session chunks, diagnostic stopping rule, fatigue weights.

## 6. Innovation features and status
Status is what the repository can demonstrate today, not what the repository contains. "Code done" means the code compiles in CI and its logic has tests; it does not mean anyone has seen it run. Rows that still need a phone, a model or an expert are named as such.

| ID | Feature | Status (Stream C view) |
| --- | --- | --- |
| C2, C3 | Skill graphs, misconception-tagged question bank | Shipped as data, pending expert review (C17) |
| C4, C5 | Knowledge tracing, planner and re-planner | Shipped in Kotlin, 61 checks (engine and LLM orchestration) in CI |
| C6, C7 | Forecast and recovery re-plan | Shipped in Kotlin with tests; neither has been tried on a real attempt history |
| I1 | Hint ladder and confidence calibration (logic) | Shipped as tested engine logic; the interface exists but has never been rendered |
| I3 | Learner simulator and evaluation harness | Shipped (headless, seeded, one command) |
| I6 | Missed-day recovery | Shipped in Kotlin; simulator shows no gain over an ordinary re-plan (see 8) |
| I2 | Thermal and memory governor | Code done: device monitor, three power modes, token/thread/context limits, cached-only fallback; no phone has run it |
| I4 | Syllabus compiler | Code done: text to a validated skill-graph draft with an editable review step; no expert review, no image input (see I5) |
| I5 | Scan a page into a quiz | Not implemented. OCR needs a dependency and licence decision, and a merged-manifest check to keep the no-INTERNET claim; see section 11 |
| I7 | Analogies and examples | Code done as grounded prompt plus template fallback; quality is unmeasured without the model |
| I8 | Teach-back check | Code done (rubric prompt, stored teach-backs); agreement with a human grader is unmeasured |
| X1 | Cognitive-load pacing | Prototype (scripted tests only, thresholds untuned) |
| X2 | Voice in and out | Prototype: platform recognizer and TTS wrappers, marked as untested on every screen that uses them |
| X3 | Offline plan sharing | Code done: lossless compressed share code with tests, QR draw and screenshot decode, teacher heatmap; nothing scanned on a phone |
| X4 | KV-cache reuse | Not started |
| X5 | On-device LoRA | Not started |

## 7. Performance, energy and thermal results
**[TO FILL by Stream A and the device table: tokens/sec, time to first token, RAM, energy per token, 20-minute thermal test, compared with the mentors' reference numbers (about 22+ tokens/sec, under 1.8 GB RAM).]**

## 8. Simulator and LLM evaluation results
Setup: 8 learner types x 1,000 learners (500 per domain), 12-week horizon, 6 hours per week, a goal skill with 10 to 24 skills in its prerequisite closure. The same learners and the same attendance are used in every condition (paired comparison). Hidden true knowledge, forgetting, slips, guesses and held misconceptions are simulated; **all learner parameters are assumptions, adjusted once for feasibility (stability 12 to 40 days, rate 1.5 to 2.0, because no planner could reach the target with the first values) and not tuned to favour any planner.** Results compare planners with each other; they are not evidence about real students. Full tables: `docs/results/results.md`; charts: `docs/results/*.png`.

**Table 1 (pooled, mean ± 95% CI, from `docs/results/results.md`).**

| Condition | Reached target | True mastery at end | Retention day 30 | Misconceptions resolved | Prerequisite violations |
| --- | --- | --- | --- | --- | --- |
| Full Sahay planner | 15.7% | 0.64 | 0.55 | 67.8% | 0 |
| Fixed linear roadmap | 5.0% | 0.53 | 0.42 | 32.1% | 0 |
| Topic-only (no misconception engine) | 17.4% | 0.65 | 0.55 | 35.2% | 0 |
| No forgetting curve | 0.1% | 0.39 | 0.31 | 54.2% | 0 |
| No re-planning | 6.0% | 0.46 | 0.40 | 15.7% | 0 |
| No diagnostic | 14.5% | 0.64 | 0.54 | 64.7% | 0 |
| Random order | 0.0% | 0.37 | 0.30 | 32.4% | 6.9 |

What the simulation supports: against the fixed roadmap the full planner ends with higher true mastery (+0.12, paired CI about ±0.00) and higher retention at day 30 (+0.12), most strongly for slow, forgetful and anxious learners; the forgetting curve with revision (-0.26 mastery when removed) and re-planning (-0.18) are the largest contributors; the misconception engine resolves about twice as many hidden misconceptions (67.8% vs 35.2%); the plan never violates a prerequisite.

What it does not support, stated plainly:
- **The PRD target of at least 20% fewer hours to target mastery is not met.** Mean study hours are within 6% of the fixed roadmap because most learners never reach 0.8 in 12 weeks; the gain appears as higher mastery for similar hours.
- **Mastery-estimate AUC is 0.64, below the 0.70 target.**
- **The misconception engine does not raise end mastery** in this model (difference +0.004 in favour of removing it); it resolves misconceptions but the extra repair time offsets the gain.
- **The diagnostic** asks 12.1 questions on average (target 10 to 15) and places learners well (AUC 0.83), but takes 15.6 simulated minutes against the 12-minute target; the real figure comes from the pilot (`docs/results/diagnostic.md`).
- **Recover mode (knapsack) is no better than an ordinary re-plan** after a 7-day gap (mastery 0.48 vs 0.50, difference -0.014); both lose about 0.10 against a learner with no gap. It does list what it dropped and why and never exceeds the declared hours.
- The LLM-only planner baseline and all LLM-harness numbers (JSON validity, explanation quality, Hindi quality, teach-back agreement, base vs tuned) are **[TO FILL: Stream A]**.

## 9. Pilot findings and device coverage
No phone has run the app yet, so this section is empty of measurements by design. The kit for filling it exists: `docs/TESTING.md` has the install and smoke script, `docs/BETA.md` the volunteer rules and what a tester is asked to record, `docs/DEMO.md` the recording script, and `bench/devices.md` the device table with one row per phone. Nothing in those files is a result. **[TO FILL: device table (target 8 Arm phones), placement accuracy and plan realism feedback, diagnostic duration, airplane-mode result per phone.]**

## 10. Privacy and offline design
The source `AndroidManifest.xml` declares no INTERNET permission, and CI fails the build if one appears; that check reads the source manifest only, because the merged manifest needs the Android SDK, so a merged-manifest assertion is still outstanding. Learner data is a JSON snapshot in the app's private storage: nothing is uploaded, the only outbound paths are the user's own clipboard and the system share sheet, and delete-all removes the file. Voice uses the speech services already installed on the phone, and plan sharing is a local QR/text code. The simulator uses only synthetic learners; the repository contains no model weights or secrets. **[TO FILL: merged-manifest evidence and an airplane-mode recording.]**

## 11. Limitations, honest failures and what we cut
- Simulator parameters are assumptions; no result here is evidence about real learners until the pilot.
- No screen in this report has been rendered. The Android module compiles in CI, and that is the whole of the UI evidence; every statement about layout, readability or interaction is unverified.
- Nothing has been installed on a phone: the ExecuTorch runtime, the governor, voice, QR scanning and the store's behaviour across an app restart are all unverified.
- Optical character recognition (I5) was not implemented. It needs a dependency and licence decision and a merged-manifest check before the no-INTERNET claim can survive it, and there was no camera or device to test it on.
- The X4 KV-cache work and the X5 LoRA work were not started.
- Content was drafted by the team lead with AI assistance and has not yet been reviewed by a subject expert; resources are listed as titles without links.
- Notes are checked at 110 to 225 words as a proxy for the 150 to 300 token target.
- The hint ladder, calibration and fatigue pacing affect only the real interface and are not measured by the simulator.
- Items listed in section 8 as not met or not shown.
- **[TO FILL: features cut by Gate 5 from the cut order.]**

## 12. Phase 2 plan
On-device LoRA training, more domains, the four additional generation languages after quality tests, NPU delegate path, and replacing assumed simulator parameters with fitted pilot data. **[TO FILL: align with Streams A and B.]**
