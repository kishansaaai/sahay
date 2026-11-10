# Sahay: an on-device adaptive learning companion
Technical report, Arm Bharat AI-SoC Challenge 2nd Edition, Problem Statement 4. **DRAFT** (Stream C sections written; sections marked **[TO FILL]** need numbers or text from Streams A and B and from the pilot).

Section numbers follow PRD 17.2. Every simulator number below is reproduced by `python simulator/run_all.py` (seeded, about 20 minutes on a laptop); every engine claim is covered by `python -m unittest discover -s engine/tests`.

## 1. Problem and users
Learners in Tier-2 and Tier-3 cities study on mid-range Android phones with patchy connectivity and no personal mentor. Generic chatbots ignore a learner's exact gaps and need a network; classroom curricula ignore pace. Sahay builds and re-plans a study roadmap on the phone, with no INTERNET permission. Personas and scenarios: PRD sections 4 and 13. **[TO FILL: one paragraph from the pilot on who actually used it.]**

## 2. System overview and architecture
Four layers and nine components (PRD section 7): Compose UI, a deterministic learning engine, an LLM orchestrator, device services, ExecuTorch runtime (Llama 3.2 1B INT4), thermal governor, Room storage and a laptop-side simulator that reuses the engine and the content files. **[TO FILL: architecture diagram export from the PRD; Streams A and B to confirm what shipped.]**

## 3. Why a deterministic engine plus on-device Llama
A bare 1B model writes fluent but unreliable study plans: it can order topics against their prerequisites, mis-track mastery and invent facts. The split used here keeps every decision (what to study, in what order, when to revise, which misconception to repair) in plain, unit-tested code, and uses Llama only for language: explanations, hints, analogies and plan wording, always grounded in a skill's reference notes and validated with retry and template fallback (PRD 10.4). In the simulator the planner never produced a prerequisite violation in 8,000 learners (Table 1), while a random order averaged 6.9 violations per learner; a model-ordered baseline is **[TO FILL: needs the on-device model, not run]**.

## 4. Model export, quantization and ExecuTorch integration on Arm
**[TO FILL by Stream A: export recipe, INT4/INT8, XNNPACK, JNI, streaming, stop latency.]**

## 5. Learning engine: knowledge tracing, misconceptions, planner
Implemented as specified in PRD 9 (`engine/`, Python reference; the Kotlin port is tracked separately).

- **Knowledge tracing (C4):** Bayesian knowledge tracing per skill with slip 0.10, learning rate 0.15 and guess set from the option count; answers after hints count with weights 1.0, 0.7, 0.4 and 0 by scaling the update toward the prior. Hand-computed reference values for the update equations are unit tests.
- **Forgetting:** effective mastery is p times exp(-dt/S); stability S grows by 1.5 to 2.5 times on a successful review (scaled by confidence and speed) and halves, minimum one day, on a failed one; revision is due when effective mastery falls below 0.7.
- **Misconception engine:** each wrong option maps to a misconception; confident wrong answers weigh twice a guess; two confident hits create a repair item placed before the skills that depend on it; a misconception is resolved after three correct answers on questions where it was a distractor, spread over at least two days; a relapse reopens it.
- **Planner:** target set, need = gap x weight / hours, topological order with highest need first, fill to the declared hours per day, repair before the skill it unblocks, spaced revision, interleaving; it returns a reason string. Over 1,000 random learners and goals every plan was prerequisite-valid, stayed within the daily minutes and deadline, and the slowest plan took far under the 1 second budget (a typical plan about 3 ms).
- **Content (C2, C3):** two domains, DSA for placements and ML engineering, 35 skills each (70 total), 6 questions per skill (420 total), 3 named misconceptions per skill (210 total); every wrong option maps to one; validated in CI (acyclic graphs, exactly one correct option, no duplicate options).

Assumptions the PRD leaves open are marked `ASSUMPTION` in the code: initial stability 2 days, reference response time 45 s, 30-minute session chunks, diagnostic stopping rule, fatigue weights.

## 6. Innovation features and status
Status is what the repository can demonstrate today. Stream C rows are written here; other rows **[TO FILL]** by their owners at Gate 4.

| ID | Feature | Status (Stream C view) |
| --- | --- | --- |
| C2, C3 | Skill graphs, misconception-tagged question bank | Shipped as data, pending expert review (C17) |
| C4, C5 | Knowledge tracing, planner and re-planner | Shipped as tested reference engine; Kotlin port pending |
| I1 | Hint ladder and confidence calibration (logic) | Shipped as tested reference logic; UI and LLM hints with Streams A and B |
| I3 | Learner simulator and evaluation harness | Shipped (headless, seeded, one command) |
| I6 | Missed-day recovery | Shipped as reference logic; simulator shows no gain over an ordinary re-plan (see 8) |
| X1 | Cognitive-load pacing | Prototype (scripted tests only, thresholds untuned) |
| I2, I4, I5, I7, I8, X2 to X5 | Governor, syllabus compiler, analogies, teach-back, scan, voice, QR, KV-cache, LoRA | **[TO FILL: Streams A and B]** |

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
**[TO FILL by Stream B and the beta programme: device table (target 8 Arm phones), placement accuracy and plan realism feedback, diagnostic duration.]**

## 10. Privacy and offline design
No INTERNET permission, checked in CI; learner data stays in the on-device database; the simulator uses only synthetic learners; the repository contains no model weights or secrets. **[TO FILL: Stream B for manifest check evidence and airplane-mode demo.]**

## 11. Limitations, honest failures and what we cut
- Simulator parameters are assumptions; no result here is evidence about real learners until the pilot.
- The engine exists as a tested Python reference; the Android module needs the Kotlin port and its tests.
- Content was drafted by the team lead with AI assistance and has not yet been reviewed by a subject expert; resources are listed as titles without links.
- Notes are checked at 110 to 225 words as a proxy for the 150 to 300 token target.
- The hint ladder, calibration and fatigue pacing affect only the real interface and are not measured by the simulator.
- Items listed in section 8 as not met or not shown.
- **[TO FILL: features cut by Gate 5 from the cut order.]**

## 12. Phase 2 plan
On-device LoRA training, more domains, the four additional generation languages after quality tests, NPU delegate path, and replacing assumed simulator parameters with fitted pilot data. **[TO FILL: align with Streams A and B.]**
