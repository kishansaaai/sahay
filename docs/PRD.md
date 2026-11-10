# PRD: Sahay, an On-Device Adaptive Learning Companion (Bharat AI-SoC Challenge PS4)

Nov 10, 2026 · @SAIKISHAN

## 1. Product overview

Sahay (working name) is an Android app that builds a personal study roadmap and re-plans it after every quiz, using a quantized Llama model that runs entirely on the phone through ExecuTorch, with no internet permission at all.

**One-line pitch:** an adaptive, misconception-aware learning companion that also senses and adapts to the phone it runs on.

**Problem.** Classroom curricula and generic chatbots ignore each learner's pace, interests and exact knowledge gaps. Cloud tutors need connectivity and send student data off the device. A bare small LLM, on the other hand, produces fluent but unreliable study plans.

**Our approach.** The learning logic (skill graph, knowledge tracing, scheduling) is deterministic and testable. Llama does what a small language model is good at: conversation, explanations, question generation, grading free text and writing the plan in the learner's language. This split is our main answer to the "pedagogical quality" criterion.

**Headline differentiators**

1. Misconception-aware gap analysis: every wrong option maps to a specific misconception, and the roadmap repairs misconceptions, not just topics.
2. Knowledge tracing with a forgetting curve: mastery per skill rises with evidence and decays with time, so revision is scheduled automatically.
3. Socratic hint ladder with confidence calibration: the tutor escalates hints and tracks over- and under-confidence as its own skill.
4. Syllabus compiler: a photographed syllabus becomes a skill graph, so any exam or course works.
5. Thermal, battery and energy governor: the app adapts generation to the SoC's state and reports joules per token and battery cost per study session.
6. Digital-twin learner simulator: thousands of synthetic learners test the planner offline and produce measured results for the report.
7. Explain-in-my-world analogies in Hindi and English, plus scan-to-quiz, teach-back grading, missed-day recovery, voice, and offline QR sharing.

The name "Sahay" is a placeholder; rename freely.

## 2. Challenge constraints and how we score

Phase 1 is due on 23 November 2026 and must be a GitHub repository with source code, a working Android app, a demo video and a technical report, submitted through the organisers' form. Constraints below come from the challenge page and the PS4 mentoring video.

**Hard requirements for PS4**

- A Llama-family model must handle the advisory and reasoning component, deployed on-device with ExecuTorch as the reference runtime.
- Target platform is an Arm-based Android smartphone.
- Capabilities: understand learner goals, identify skill gaps, recommend learning pathways, generate study plans, adapt over time.
- Deliverables: source code, application, demonstration video, technical report.

**Judging criteria (from the mentoring video) and our answer**

| Criterion | What judges check | Our answer |
| --- | --- | --- |
| Technical innovation | Effective use of ExecuTorch and Llama on Arm Android | INT4 Llama 3.2 1B via ExecuTorch with JNI, energy and thermal governor, prompt/KV-cache experiments, full benchmark suite |
| Real-world viability | Smooth responsiveness and offline readiness | Streaming UI, no INTERNET permission, airplane-mode demo, tested across many phones |
| Pedagogical quality | Accurate skill gap analysis, coherent roadmap | Skill graph, misconception engine, knowledge tracing, simulator-backed evaluation |
| Social impact | Accessible, personalised education | Hindi and English generation, syllabus compiler for any exam, offline QR classroom sharing |

**Reference targets quoted by the mentors:** about 22+ tokens/sec on Arm and under 1.8 GB RAM for an INT4 Llama. We treat these as targets to compare against and report our own measured values honestly.

**Rules that shape the build**

- Teams have 1 to 3 members; the code must be the original work of the registered team. Helpers (testers, reviewers, pilot learners) are fine, but they do not write the code.
- Every dataset, library and model must be credited, and Llama's licence terms must be accepted.
- The page lists Hindi, Tamil, Telugu, Bengali, Marathi and English in the mentor slides. Llama 3.2's official language list should be checked on its model card; we plan to ship Hindi and English generation and localise the UI for the rest.

**Key dates**

| Milestone | Date |
| --- | --- |
| Phase 1 submission deadline | 23 Nov 2026 |
| Our internal target to submit | 22 Nov 2026 |
| Phase 1 results | Mid-December 2026 |
| Phase 2 PoC deadline | 5 Feb 2027 |
| Finals | 15 to 16 Feb 2027 |

## 3. Goals, non-goals and success metrics

The primary goal is to win PS4 by being the most reliable on-device demo, backed by measured evidence on every judging criterion. Success is defined by numbers we can print in the report.

**Goals**

- G1. A fully offline Android app that goes from learner goal to roadmap to adaptive re-plan using on-device Llama.
- G2. Learning logic that is measurably better than a fixed roadmap, proven in simulation and a small pilot.
- G3. A complete, honest on-device performance profile: speed, memory, energy, heat.
- G4. A polished demo video and report that map directly onto the four judging criteria.

**Non-goals for Phase 1**

- No cloud backend, accounts or sync.
- No on-device LoRA training (we fine-tune offline and describe on-device training as Phase 2).
- No claim of full-quality generation in Tamil, Telugu, Bengali or Marathi unless our tests support it.
- No content library for every subject; we ship two polished domains and a syllabus compiler for the rest.

**Success metrics**

| Metric | Target | How measured |
| --- | --- | --- |
| Generation speed | At or above 15 tokens/sec on the main test phone (mentor reference is 22+) | Benchmark harness, 10 runs, median |
| Time to first token | Under 3 seconds for a typical tutoring prompt | Benchmark harness |
| Peak RAM with model loaded | Under 2.0 GB (mentor reference is under 1.8 GB) | Android profiler / dumpsys |
| Valid JSON from the model | At least 98% after validate-and-retry | Evaluation harness, 500 prompts |
| Roadmap prerequisite correctness | 100% of roadmaps respect the skill graph | Automated check, never LLM-judged |
| Adaptive planner vs fixed roadmap | At least 20% fewer simulated study hours to reach target mastery | Learner simulator |
| Mastery prediction quality | AUC of at least 0.70 on held-out simulated and pilot answers | Evaluation harness |
| Offline proof | App declares no INTERNET permission and works in airplane mode | Manifest check, demo |
| Sustained performance | Governor keeps tokens/sec within 25% of peak over a 20-minute session | Thermal test |
| Device coverage | At least 8 distinct Arm phones benchmarked | Beta-tester device table |
| Crash-free demo | Zero crashes across 50 scripted demo runs | Scripted test |

These targets are our own and may be adjusted once the first benchmarks exist.

## 4. Users and scenarios

Our primary user is a college student in a Tier-2 or Tier-3 city who studies on a mid-range Android phone with patchy data and has no personal mentor. Three personas drive the design and the demo.

| Persona | Situation | What Sahay does for them |
| --- | --- | --- |
| Aarav, 20, 3rd-year CS student | Wants a placement-ready DSA and ML path in 10 weeks, 6 hours a week, studies in English with Hindi comfort | Diagnostic quiz, misconception-targeted roadmap, weekly re-plan, analogies from cricket |
| Meena, 24, GATE aspirant | Has a 90-page official syllabus and a day job | Photographs the syllabus, gets a skill graph, spaced revision, missed-day recovery |
| Rohit, 17, Class 12 student in a low-connectivity town | Shares one phone with family, prefers Hindi | Hindi explanations, hint ladder instead of answers, offline everything |

A fourth, secondary user is a **teacher** who wants to see where a class is stuck without any server; the offline QR classroom view serves them.

**Core scenarios**

1. First run: choose language, state goal, level and weekly hours, take a 10-minute diagnostic, receive a roadmap.
2. Daily use: open Today, study a lesson, answer practice questions with confidence ratings, get hints, finish with an updated plan.
3. Stuck on a concept: ask the tutor in chat, get a Socratic hint ladder and a personalised analogy.
4. Real-life disruption: miss four days, tap Recover, and see the plan re-packed around the deadline.
5. New course: photograph a syllabus or a textbook page, get a skill graph or a quiz in under a minute.
6. Hot or low-battery phone: the app visibly switches to a lighter generation mode and explains why.
7. Classroom: a student shows a QR, the teacher's phone aggregates a misconception heatmap.

## 5. Feature map

There are 24 features in four layers; the base layer and the learning core are mandatory and everything else is ranked so we know what to cut first. Priority P0 means the demo fails without it, P1 means a strong differentiator, P2 means a bonus. Status is the realistic Phase 1 outcome we will report.

| ID | Feature | Layer | Priority | Target status |
| --- | --- | --- | --- | --- |
| B1 | ExecuTorch + INT4 Llama 3.2 1B export for Android | Base | P0 | Shipped |
| B2 | JNI wrapper with token streaming and cancel | Base | P0 | Shipped |
| B3 | Model manager (load, unload, warm-up, memory guard) | Base | P0 | Shipped |
| B4 | No INTERNET permission, fully offline | Base | P0 | Shipped |
| C1 | Goal intake dialogue (goal, level, hours, interests, language) | Core | P0 | Shipped |
| C2 | Skill graph for two polished domains | Core | P0 | Shipped |
| C3 | Misconception-tagged diagnostic and question bank | Core | P0 | Shipped |
| C4 | Knowledge tracing with forgetting curve | Core | P0 | Shipped |
| C5 | Roadmap planner and adaptive re-planner | Core | P0 | Shipped |
| C6 | Conversational tutor chat | Core | P0 | Shipped |
| C7 | Progress dashboard and mastery heatmap | Core | P0 | Shipped |
| I1 | Socratic hint ladder with confidence calibration | Innovation | P1 | Shipped |
| I2 | Thermal, battery and energy governor | Innovation | P1 | Shipped |
| I3 | Digital-twin learner simulator and evaluation harness | Innovation | P1 | Shipped |
| I4 | Syllabus compiler (photo to skill graph) | Innovation | P1 | Shipped or Prototype |
| I5 | Explain-in-my-world analogies, Hindi and English | Innovation | P1 | Shipped |
| I6 | Missed-day recovery replanner | Innovation | P1 | Shipped |
| I7 | Teach-back grading | Innovation | P1 | Shipped |
| I8 | Scan-to-quiz with on-device OCR | Innovation | P1 | Shipped or Prototype |
| X1 | Cognitive-load-aware pacing | Extended | P2 | Prototype |
| X2 | Offline voice (speech to text, text to speech) | Extended | P2 | Prototype |
| X3 | Offline QR roadmap sharing and classroom heatmap | Extended | P2 | Shipped or Prototype |
| X4 | Prompt prefix and KV-cache reuse experiment | Extended | P2 | Prototype, depends on runtime support |
| X5 | Offline LoRA fine-tune of tutoring style and JSON reliability | Extended | P2 | Prototype; on-device training is Phase 2 |

Two items need an early check with the mentors (tech-queries@armbharatchallenge.com, subject line starting "PS4"): whether the ExecuTorch Android runner supports prefix reuse, and whether any LLM fine-tuning path on device is realistic. Both are marked as experiments for that reason.

## 6. Functional requirements and acceptance criteria

Each requirement has a testable acceptance criterion so that "done" is never a matter of opinion. IDs match the feature map.

### Base layer

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| B1 | Export Llama 3.2 1B Instruct with INT4 weight quantization to an ExecuTorch .pte file and run it on Arm CPU with the XNNPACK backend | Model loads on the test phone and answers a fixed 20-prompt set; export script is in the repo and reproducible |
| B2 | JNI wrapper exposes load, generate (streaming callback), stop and unload | UI shows tokens as they arrive; stop halts generation within 500 ms; no memory growth over 200 consecutive generations |
| B3 | Model manager loads lazily, warms up on app start, and unloads on low-memory signals | App survives a simulated onTrimMemory without a crash and reloads on next use |
| B4 | Manifest declares no INTERNET permission | Static check in CI fails the build if the permission appears |

### Learning core

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| C1 | Intake dialogue collects goal, current level, hours per week, deadline, interests and language through chat plus quick-reply chips | Produces a valid LearnerProfile JSON; free-text goals are mapped to a domain with at least 90% accuracy on 30 test goals |
| C2 | Skill graph with prerequisites, difficulty, estimated hours and resources for two domains (suggested: DSA for placements, and ML engineering) | At least 35 skills per domain; graph is acyclic; every skill has at least 6 questions |
| C3 | Diagnostic of 10 to 15 adaptive questions; every distractor maps to a misconception id | Each question has a misconception tag per wrong option; diagnostic ends within 12 minutes on average in pilot |
| C4 | Knowledge tracing updates mastery per skill after every answer and decays it over time | Unit tests match the reference equations in section 9; mastery never leaves 0 to 1 |
| C5 | Planner orders skills by prerequisites, gap size and available hours; re-planner runs after each session or on demand | 100% prerequisite-valid roadmaps across 1,000 simulated learners; replan completes in under 1 second excluding LLM text |
| C6 | Tutor chat answers concept questions with streamed Llama output grounded in the skill's reference notes | Answers cite the current skill; refuses to drift off-topic in more than 95% of 50 adversarial prompts |
| C7 | Dashboard shows mastery heatmap, streak, forecast to goal and upcoming revisions | Updates within 1 second of finishing a quiz; works in portrait and landscape |

### Innovation layer

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| I1 | Hint ladder: nudge, hint, worked step, solution; learner rates confidence before answering; calibration score tracked | Ladder level and confidence are stored per attempt; calibration chart appears on the dashboard |
| I2 | Governor reads thermal status, battery level and charging state and changes generation parameters | State changes are logged and visible in a debug panel; sustained-run test meets the 25% target |
| I3 | Simulator runs synthetic learner types against the planner and baselines, headless, from the command line | One command reproduces every number in the evaluation section of the report |
| I4 | Syllabus compiler converts OCR text into a skill graph draft that the learner can edit | At least 80% of topics from 5 real syllabi are captured; invalid graphs are rejected by the validator |
| I5 | Tutor adapts explanations to the learner's interests and language | Pilot A/B check on at least 20 learners; language switch preserves meaning on a 20-item test |
| I6 | Recover mode re-packs missed work around the deadline and weekly hours | Never schedules more than the declared hours; shows what was dropped and why |
| I7 | Teach-back: learner explains a concept by text or voice, rubric-based grading with specific feedback | Agreement of at least 70% with a human grader on 40 sample explanations |
| I8 | Scan a page, run on-device OCR, generate a summary and 5 questions | End-to-end under 60 seconds on the main phone; works in airplane mode |

### Extended layer

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| X1 | Estimate fatigue from response time and error streaks and shorten or ease the session | Triggers on scripted fatigue patterns in tests; never blocks the learner |
| X2 | Speech input and spoken output with offline packs | Works in airplane mode on at least 3 of the test phones; marked Prototype otherwise |
| X3 | Export a plan or anonymised progress as one or more QR codes; teacher phone merges them | Round-trip of a full roadmap through QR without data loss; class heatmap for 5 simulated students |
| X4 | Measure prompt-prefix reuse or a shortened system prompt | Report shows time to first token with and without the optimisation |
| X5 | Offline LoRA fine-tune for tutoring tone and JSON discipline, merged and exported to ExecuTorch | JSON validity rate with the tuned model compared with the base model in the evaluation harness |

## 7. System architecture

&#91;embedded content: system architecture · 4 layers, 9 components\]

The UI talks to the learning engine for every decision and to the orchestrator for every sentence. The orchestrator calls the ExecuTorch runtime, whose limits the governor sets from the phone's thermal, battery and memory signals. The simulator reuses the engine code and content files, so reported results match the app.

**Key technical choices**

- **Language and UI:** Kotlin and Jetpack Compose for the app and the engine; a small C++ JNI layer over the ExecuTorch runtime.
- **Engine isolation:** the engine is its own Gradle module with no Android dependency so the simulator can run it on a laptop.
- **Threading:** inference runs on a dedicated background thread with a single-flight queue; the UI only receives streamed tokens through a Flow.
- **Single source of truth:** every answer is stored as an Attempt row, and mastery, misconception and plan state can be rebuilt by replaying the attempts.
- **Content as data:** skill graphs, questions, reference notes and localised strings are versioned JSON files, validated in CI.
- **Build:** the model file is not stored in Git; the export script, checksums and a release asset keep the repo small and reproducible.

## 8. Data model

All data lives in an on-device SQLite database (Room) plus bundled JSON assets; nothing leaves the phone. Content assets are versioned files in the repo so the evaluation harness can load the same data as the app.

| Entity | Key fields | Notes |
| --- | --- | --- |
| Domain | id, name, language variants, skill ids | Two bundled domains plus compiled ones |
| Skill | id, name, description, prerequisites\[\], difficulty 1 to 5, est\_hours, resources\[\], reference\_notes | Graph must be acyclic; validated at load and on every compile |
| Misconception | id, skill\_id, short\_name, explanation, repair\_activity | A misconception belongs to one skill and can be shared by several questions |
| Question | id, skill\_id, stem, options\[\], correct\_option, option\_misconception\_ids\[\], difficulty, discrimination, language variants | Each wrong option points to a misconception |
| LearnerProfile | goal, domain, level, hours\_per\_week, deadline, interests\[\], language, created\_at | Created by the intake dialogue |
| SkillState | learner\_id, skill\_id, mastery p, stability s, last\_seen, attempts, confidence\_bias | Core of the learner model |
| MisconceptionState | learner\_id, misconception\_id, evidence\_count, last\_triggered, resolved | Drives repair modules |
| Attempt | id, question\_id, chosen\_option, correct, response\_ms, confidence 0 to 100, hint\_level, timestamp | One row per answer; the single source of truth for replays |
| Plan | id, version, created\_at, reason, weeks\[\] with items\[\] | Each replan creates a new version so changes can be shown |
| PlanItem | skill\_id or misconception\_id, type (learn, practice, revise, repair), scheduled\_date, est\_minutes, status | Status: planned, done, skipped, dropped |
| Session | id, start, end, energy\_mAh\_estimate, mean\_tokens\_per\_sec, governor\_events\[\] | Feeds the energy report |
| TeachBack | id, skill\_id, text, rubric\_scores, feedback | Rubric defined in section 10 |

The app can export and import a learner snapshot as JSON. The simulator and the app use the same schema, so every simulation run can be replayed inside the app for debugging.

## 9. Learning engine specification

The learning engine is plain Kotlin code with no LLM dependency, so every decision is deterministic, unit-testable and simulatable. Default parameters below are starting points that the simulator will tune.

### 9.1 Knowledge tracing (feature C4)

Each skill keeps a mastery probability p. After an answer, p is updated with Bayesian knowledge tracing, where s is the slip rate, g the guess rate and t the learning rate.

```latex
p' = \frac{p(1-s)}{p(1-s) + (1-p)g}\ \text{(correct)} \qquad p' = \frac{ps}{ps + (1-p)(1-g)}\ \text{(wrong)}
```

```latex
p_{next} = p' + (1-p')\,t
```

Defaults: initial p = 0.2, s = 0.10, g = 0.25, t = 0.15, with g set from the number of options (1 divided by option count). Answers given after hints count with a weight of 1.0, 0.7, 0.4 and 0 for hint levels 0 to 3, applied by scaling the update toward the prior.

### 9.2 Forgetting curve and revision

Every skill also has a stability S in days. Retention decays exponentially and the planner uses effective mastery, not raw p.

```latex
R(\Delta t) = e^{-\Delta t / S} \qquad p_{eff} = p \cdot R
```

On a successful review S is multiplied by a growth factor between 1.5 and 2.5 that scales with answer confidence and speed; on a failed review S is cut to 0.5 of its value (minimum 1 day). A skill is scheduled for revision when p\_eff falls below 0.7.

### 9.3 Misconception engine (feature C3)

- Choosing a wrong option adds evidence to its misconception; evidence is weighted by stated confidence, so a confident wrong answer counts about twice as much as a guess.
- Two or more confident hits on the same misconception creates a repair item placed before any skill that depends on it.
- A misconception is marked resolved after three correct answers on questions where it was a distractor, spaced over at least two days.
- Repair activities are templated (a contrast example, a counter-example, a short worked fix) and Llama personalises the wording; the activity type itself is deterministic.

### 9.4 Confidence calibration (feature I1)

Before answering, the learner moves a confidence slider from 0 to 100. The app tracks bias (mean confidence minus accuracy) and the Brier score. A bias above +0.15 labels the learner overconfident and the tutor starts asking for a justification; a bias below -0.15 triggers encouragement and a shorter ladder.

### 9.5 Planner (feature C5)

1. Build the target set: skills needed for the goal plus all prerequisites, from the goal-to-domain mapping or compiled graph.
2. For each skill compute need = (target mastery minus p\_eff) times goal weight, divided by estimated hours.
3. Produce a topological order; among skills whose prerequisites are ready, choose highest need first.
4. Fill weeks greedily within the declared hours per week, and place repair items directly before the skills they unblock.
5. Insert revision items for skills whose p\_eff will cross 0.7 before the deadline.
6. Interleave topics inside a week (no more than two sessions of the same skill in a row) and keep every day within the learner's stated minutes.

The planner returns a Plan with a reason string. Llama only turns the plan into friendly text and explanations; it never changes the order.

### 9.6 Re-planner and missed-day recovery (feature I6)

Replans run after every session, after two or more missed days, when the deadline or hours change, and on demand. In recovery mode the planner treats the remaining hours as a budget and solves a knapsack: item value is importance times mastery gap, item cost is hours, and prerequisite closure is enforced. Dropped items are listed with the reason so the learner stays in control.

### 9.7 Cognitive-load-aware pacing (feature X1)

A fatigue index combines the rise in response time against the learner's own median and the current error streak. Above a threshold the app offers a lighter activity, a break, or ends the session with a recap. This uses only on-device signals and is always dismissible.

## 10. LLM layer: model, prompts and guardrails

Llama 3.2 1B Instruct at INT4 is the production model; the 3B model is benchmarked only as a comparison. Llama handles language tasks and never decides plan order, mastery or grades that affect the plan without a deterministic check.

### 10.1 Model and runtime choices

| Decision | Choice | Reason |
| --- | --- | --- |
| Primary model | Llama 3.2 1B Instruct | Fits the mentors' RAM reference of under 1.8 GB at 4-bit and gives interactive speed on mid-range Arm phones |
| Comparison model | Llama 3.2 3B Instruct | Quality vs speed vs memory trade-off in the report |
| Quantization | 4-bit weight quantization with 8-bit activations as supported by the ExecuTorch Llama export flow; also an INT8 variant | Compare quality, speed and RAM; confirm exact recipe names against the current ExecuTorch docs |
| Backend | XNNPACK on Arm CPU, KleidiAI-accelerated kernels if the build enables them | Reliable path in 13 days; NPU delegates are a stretch experiment |
| Context cap | 2,048 tokens | Keeps KV cache memory small and latency predictable |
| Sampling | Temperature 0.2 for JSON tasks, 0.7 for chat and analogies | Stability where structure matters |

The Llama licence must be accepted before downloading weights, and the licence terms apply to any fine-tuned or distilled variant we ship.

### 10.2 Prompt design rules

- System prompt under 300 tokens and identical across calls so prefix reuse (X4) is possible.
- Every task has its own short prompt template with a schema, two examples and the grounding notes for exactly one skill.
- Grounding: the skill's reference notes (150 to 300 tokens) are pasted into the prompt by id lookup; no embedding search is needed for Phase 1.
- Output language is an explicit parameter; the prompt includes one example in that language.

### 10.3 Tasks and schemas

| Task | Input | Output | Validator |
| --- | --- | --- | --- |
| Goal parsing | Free-text goal | goal, domain\_id, target\_level, timeframe\_weeks | Domain id must exist; otherwise fall back to chips |
| Plan narration | Deterministic Plan JSON | Weekly summaries and motivational text per item | Item ids must match the plan; text length limits |
| Concept explanation | Skill id, question, interests, language | explanation, analogy, check\_question | Must mention the skill; length limits |
| Hint generation | Question, learner answer, hint level | hint text only | Must not contain the correct option text at levels 0 and 1 |
| Question drafting (offline authoring and scan-to-quiz) | Skill notes or OCR text | stem, options, answer, misconception per wrong option | Exactly one correct option; distractors unique |
| Teach-back grading | Skill id, learner text | scores for accuracy, completeness, clarity, example (0 to 3 each) and two-sentence feedback | Scores in range; feedback required |
| Syllabus compilation | OCR text | skills\[\], prerequisites\[\] | Graph must be acyclic; at most 60 skills per compile |

```json
{
  "task": "teachback_grade",
  "skill_id": "dsa.stack",
  "scores": {"accuracy": 2, "completeness": 1, "clarity": 3, "example": 0},
  "feedback": "Your definition is right. Add one real example, such as undo in an editor."
}
```

### 10.4 Validate, retry, fall back

1. Parse the output and check it against the task's JSON schema and validator.
2. On failure retry up to two times, appending the exact validation error to the prompt.
3. If it still fails, use a deterministic template (for plan text) or ask the learner a simpler quick-reply question (for goal parsing). The learner never sees a parsing error.
4. If the runtime supports grammar-constrained or structured decoding, use it for JSON tasks; otherwise rely on steps 1 to 3. We log the validity rate of each task for the report.

### 10.5 Languages

Hindi and English are the generation languages for Phase 1, because Llama 3.2 lists Hindi among its supported languages (confirm on the model card). The UI is localised for Hindi, Tamil, Telugu, Bengali, Marathi and English. For the four other Indian languages, tutor output is offered as an experimental option with a visible label, and the report includes the quality test results rather than a claim of full support.

### 10.6 Safety and quality guardrails

- The tutor stays on the learner's domain; off-topic or unsafe requests get a short refusal with a redirect.
- No medical, legal or financial advice is generated.
- The hint ladder prompt is checked so the answer is not leaked early.
- A "show source note" button reveals the grounding text behind any explanation.

### 10.7 Offline fine-tune (feature X5)

We fine-tune a LoRA adapter on Colab with a few thousand reviewed examples of tutoring tone, hint ladders and strict JSON output in Hindi and English, merge it, quantize it and export it to ExecuTorch. Training data must be reviewed and licensed for this use. The evaluation harness reports validity rate and tutoring quality of the tuned model against the base model; if the tuned model is not better, the base model ships and the result is still reported.

## 11. Performance, energy and the thermal governor

We report on-device performance as a product engineering result: speed, memory, energy and heat, measured on real phones with a fixed protocol. The governor (feature I2) turns those measurements into runtime behaviour.

### 11.1 Metrics we measure

| Metric | Unit | Method |
| --- | --- | --- |
| Time to first token | seconds | Timestamp from submit to first streamed token, 10 runs, median and p95 |
| Decode speed | tokens/sec | Tokens generated divided by decode time, fixed 200-token output |
| Prompt processing speed | tokens/sec | Fixed 500-token prompt |
| Peak and steady RAM | MB | Android memory tools while the model is loaded and generating |
| Model size on disk | MB | .pte file size |
| Energy per token | millijoules | Battery charge drop or power rail readings over a long run, divided by tokens (method stated in report; see caveat below) |
| Battery per session | percent | A scripted 20-minute study session |
| Thermal behaviour | tokens/sec over time and thermal status | 20-minute continuous run, with and without the governor |
| Quality | benchmark score or fixed task set | Same prompts across 1B/3B and INT4/INT8 variants |

Caveat: phone battery counters are coarse. We will state the measurement method and its uncertainty, repeat runs at the same starting charge and screen brightness, and prefer a USB power meter if one is available.

### 11.2 Benchmark matrix

Variants: 1B INT4, 1B INT8, 3B INT4. Phones: the main test phone plus every beta-tester device (target at least 8 distinct Arm SoCs). Conditions: cold start, warm, charging and on battery, cool and heated. The output is one table and two charts in the report.

### 11.3 Governor behaviour (feature I2)

The governor reads Android's thermal status, battery level, charging state and available memory every few seconds and selects one of four modes.

| Mode | Trigger | Changes |
| --- | --- | --- |
| Full | Cool, charging or above 40% battery | Normal limits |
| Balanced | Light thermal throttling or 20 to 40% battery | Cap output length, lower thread count by one |
| Saver | Moderate throttling or below 20% battery | Shorter answers, smaller context, defer non-urgent generation such as question drafting |
| Pause | Severe throttling or critical battery | Use cached or template text, show a cooldown message |

Every mode change is logged with a reason and shown in a small status chip so the learner understands the behaviour. Thresholds are tuned from our own thermal test. The 20-minute run with and without the governor is a headline figure in the report.

### 11.4 Optimisation experiments

- Thread count sweep to find the best number of Arm cores for decode.
- Prompt prefix and KV-cache reuse, or a shorter system prompt, measured as time-to-first-token savings (feature X4; depends on runtime support).
- Warm-up generation at app start so the first user request is fast.
- Optional stretch: an NPU or GPU delegate path on a phone whose chipset supports it, reported separately.

## 12. Evaluation plan: simulator, harness and pilot

Every claim in the report is backed by one reproducible command. The evaluation has three parts: a digital-twin simulator for the learning engine, a harness for the LLM tasks, and a small human pilot.

### 12.1 Digital-twin learner simulator (feature I3)

The simulator generates synthetic learners whose true knowledge is hidden, lets them answer questions with realistic noise, and measures how well each planner gets them to the target mastery. It runs headless on a laptop using the same Kotlin engine code as the app.

| Learner type | Behaviour modelled |
| --- | --- |
| Fast learner | High learning rate, low slip |
| Average learner | Default parameters |
| Slow learner | Low learning rate, needs repetition |
| Forgetful learner | Short memory stability |
| Overconfident guesser | Rates high confidence regardless of knowledge |
| Anxious learner | Under-rates confidence, higher slip |
| Irregular attender | Skips days at random |
| Misconception-prone | Holds two to four specific misconceptions |

Baselines compared against the full planner: (a) a fixed linear roadmap, (b) a topic-only planner with no misconceptions, (c) a planner without the forgetting curve, (d) an LLM-only planner that asks Llama to order the topics, and (e) random order. For (d) we run the real on-device model on a sample of learners so the comparison is honest.

### 12.2 Metrics

| Metric | Meaning |
| --- | --- |
| Hours to target mastery | Simulated study hours until average true mastery reaches the target |
| Retention at day 30 | True mastery 30 days after the plan completes |
| Misconception resolution rate | Share of hidden misconceptions resolved |
| Prerequisite violations | Count of skills scheduled before their prerequisites (must be zero for ours) |
| Mastery estimate quality | AUC of predicted correctness against simulated truth |
| Plan stability | Fraction of items that move after a replan, to avoid an erratic experience |
| Recovery quality | Mastery reached after a 7-day gap with and without recover mode |

Each experiment runs at least 1,000 learners per type with fixed random seeds, and results report means with confidence intervals. Ablations remove one feature at a time to show what each contributes.

### 12.3 LLM evaluation harness

- JSON validity rate per task before and after validate-and-retry, on 500 prompts per task.
- Prompt sets are fixed in the repo; runs are reproducible on a laptop with the same exported model, and a subset runs on the phone.
- Explanation quality is scored with a 4-point rubric (correct, clear, grounded, appropriate level) by two human reviewers on 100 samples.
- Hindi quality is checked by two native speakers on 50 samples against the English originals.
- Teach-back grading agreement with a human grader on 40 explanations.
- Comparison of base and offline-tuned model (feature X5) and of 1B and 3B.

### 12.4 Human pilot

We invite 20 to 30 learners from the wider volunteer group, with their consent, to use the app for a week with anonymised, on-device logs shared only by their choice. Questions: did diagnostic placement feel accurate, was the plan realistic, did analogies help, was the app responsive. Pilot learners and testers do not write code, in line with the team rules. We also use the group for the device table (at least 8 different Arm phones) and for Hindi review.

## 13. User experience

The app should feel like a calm daily coach: one clear next action on the home screen, instant streaming responses, and every adaptation explained in one plain sentence. Build with Jetpack Compose and Material 3.

### 13.1 Screens

| Screen | Purpose | Key elements |
| --- | --- | --- |
| Welcome and language | Pick language, explain offline privacy | Language chips, "no internet permission" badge |
| Intake chat | Collect goal, level, hours, deadline, interests | Chat with quick-reply chips; progress dots |
| Diagnostic | Adaptive quiz | Confidence slider, timer-free by default, hint ladder button |
| Roadmap | Weekly plan with reasons | Week cards, repair and revise tags, "why this" sheet |
| Today | One next action plus a short list | Start button, minutes estimate, streak |
| Study and practice | Lesson, questions, hints | Streamed explanation, analogy toggle, check question |
| Tutor chat | Free questions | Streaming answers, source-note button, voice button |
| Dashboard | Progress | Mastery heatmap, forecast to goal, calibration chart, misconception list |
| Recover | Missed-day replan | Before and after plan, dropped items with reasons |
| Scan | Syllabus and page capture | Camera, OCR preview, edit before generate |
| Teach-back | Explain a concept | Text or voice, rubric feedback |
| Share | QR export, class heatmap | QR display and scan, teacher view |
| Device and energy | Governor status and benchmarks | Mode chip, tokens/sec, battery cost, debug log |
| Settings | Language, model variant, data export and delete | One-tap delete of all data |

### 13.2 UX principles

- First token on screen within 3 seconds, and a visible stop button on every generation.
- Never show raw errors from the model; fall back gracefully (section 10.4).
- Every plan change shows one sentence on why it changed.
- Accessible by default: large text support, TalkBack labels, high-contrast theme, one-handed layout.
- Hindi and English are first-class; the language can be switched at any time without losing state.

### 13.3 The 5-minute demo path

1. Turn on airplane mode and show the manifest badge.
2. Intake in Hindi: a goal turns into a profile within seconds.
3. Diagnostic with confidence sliders; show a misconception being caught.
4. Roadmap appears with reasons; open "why this".
5. Answer wrongly on purpose; the plan visibly adapts and a repair item appears.
6. Ask the tutor for a cricket analogy; show the hint ladder.
7. Photograph a syllabus page; a skill graph appears.
8. Show the energy panel and the thermal chart with the governor on and off.
9. Close on the simulator results slide and the benchmark table.

## 14. Privacy, offline and security requirements

Privacy is a verifiable property of the build, not a promise: the app has no network permission, stores everything locally and lets the learner delete it in one tap.

- **No INTERNET permission** in the release manifest, enforced by a CI check (B4). Model weights are bundled with the APK or pushed once by file transfer, so no download code exists.
- **Local-only storage.** SQLite database in app-private storage, with Android's file-based encryption relying on the device lock; optional app PIN for shared phones such as Rohit's.
- **No analytics or crash-reporting SDKs.** Benchmarks and pilot logs are exported only by an explicit, learner-triggered file share.
- **QR sharing sends only what the learner chooses**, shows a preview of the contents, and strips names by default.
- **Camera and microphone** permissions are requested only when the scan or voice feature is first used, with a one-line reason.
- **Data rights.** Export a snapshot as JSON and delete everything from Settings.
- **Model supply chain.** Record the SHA-256 of every shipped model file and verify it at load; document the Llama licence and every third-party library licence in the repo.
- **Prompt-injection hygiene for scanned text.** OCR text is treated as data inside delimiters, never as instructions, and output is schema-validated before it can change any state.

## 15. Team, schedule and process

Three registered members own the code, split by workstream, with a wider volunteer group providing testing and review but no code. The schedule below is gated: each gate has a pass criterion, and a missed gate triggers the cut order in section 16.

### 15.1 Roles

| Role | Owns | Main deliverables |
| --- | --- | --- |
| A: Runtime and ML lead | ExecuTorch export, JNI, governor, benchmarks, offline LoRA, KV-cache experiment, LLM evaluation harness | B1 to B3, I2, X4, X5, section 11 results |
| B: Android lead | Compose UI, camera OCR, voice, QR sharing, charts, release build, manifest check | C7, I8, X2, X3, UI polish, APK |
| C: Learning engine lead | Skill graphs, question bank, knowledge tracing, planner, simulator, teach-back rubric, report editing | C2 to C5, I1, I3, I6, I7, section 12 results |
| Volunteer group (no code) | Beta testing on many Arm phones, Hindi review, subject review of questions, pilot learners, demo video support | Device table, review sign-offs, pilot data |
| Faculty mentor | Review of question bank, report and demo | Sign-off notes |

All three leads share intake chat, tutor chat, hint ladder wiring and the final report; any lead may pick up another's task when blocked.

&#91;embedded content: schedule · 9 work streams, 6 milestones, 10 to 22 Nov 2026\]

**Gate criteria**

1. **Gate 1, 11 Nov:** the official ExecuTorch Llama Android demo runs on a real phone.
2. **Gate 2, 14 Nov:** our INT4 1B model streams tokens in our own app and first benchmark numbers exist.
3. **Gate 3, 17 Nov:** goal intake, diagnostic, roadmap, a wrong answer and an adapted plan work end to end, offline.
4. **Gate 4, 19 Nov:** every innovation feature is marked Shipped, Prototype or Cut, and beta testers have the APK.
5. **Gate 5, 21 Nov:** feature freeze; from here only bug fixes, benchmarks, video and report.

### 15.2 Working process

- 15-minute standup every day, plus a written status line per feature in the repo (Not started, In progress, Done, Cut).
- Trunk-based Git with short branches; every pull request is reviewed by one of the other two leads.
- Continuous integration on every push: unit tests, content validation (acyclic graphs, one correct option per question), the manifest check and a debug APK build.
- A daily integration build installed on the main test phone, so integration problems are found within 24 hours.
- Mentor contact: questions go to tech-queries@armbharatchallenge.com with "PS4" in the subject, batched once a day.

## 16. Risks, fallbacks and cut order

The biggest risk is not any single feature but a half-finished product on 22 November, so every risk has a trigger date and a fallback, and there is a fixed order in which features get cut.

| Risk | Likelihood | Impact | Mitigation and fallback |
| --- | --- | --- | --- |
| ExecuTorch export or JNI integration blocks progress | Medium | Critical | Start from the official Llama Android demo app; Gate 1 on 11 Nov; ask the mentors the same day if blocked |
| 1B model output is too weak or unreliable | Medium | High | Deterministic engine owns structure; validate-and-retry; templates as fallback; offline LoRA if time allows |
| Speed or RAM below target on test phones | Medium | High | Thread sweep, shorter context, INT8 vs INT4 comparison; report honestly against mentor reference numbers |
| Phone overheats during demo | Medium | Medium | Governor, cooldown before recording, recorded benchmark run as backup |
| Hindi output quality is poor | Medium | Medium | Native-speaker review; template-first Hindi text; label limits openly |
| OCR quality is poor on camera photos | Medium | Medium | Edit-before-generate screen; test on real syllabi; mark feature Prototype if it misses the 80% target |
| Scope creep from 24 features | High | High | Feature freeze on 21 Nov; cut order below; every feature has a status in the report |
| Merge conflicts and integration bugs | Medium | Medium | Trunk-based workflow, daily integration build, CI running unit tests and the manifest check |
| Disqualification risk from team or IP rules | Low | Critical | Only the registered members write code; credit all assets and licences; confirm the rules by email |
| Submission form or upload fails near deadline | Low | High | Submit on 22 Nov, keep a release tag and a backup APK link |
| Test devices behave differently | Medium | Low | Beta tests start on 18 Nov; a documented known-issues list |

**Cut order if time runs short** (cut from the top first):

1. X5 offline LoRA (keep as described future work)
2. X2 voice
3. X4 prefix and KV-cache experiment
4. X3 QR classroom heatmap (keep plain QR plan export if possible)
5. I8 scan-to-quiz
6. X1 cognitive-load pacing
7. I7 teach-back
8. I4 syllabus compiler
9. I2 governor (keep the measurement panel, drop automatic modes)

Never cut: B1 to B4, C1 to C7, I1, I3, I5 and I6, the benchmark suite, the demo video and the report.

## 17. Deliverables and submission checklist

The submission is one public GitHub repository, plus the form entry; everything below must exist and be tested from a clean clone before 22 November.

### 17.1 Repository layout

```text
sahay/
  README.md              what it is, how to build, demo GIF, benchmark summary
  LICENSES/              Llama licence, library licences, dataset credits
  app/                   Android app (Kotlin, Compose, JNI to ExecuTorch)
  engine/                learning engine (Kotlin module, no Android dependency)
  simulator/             learner simulator and baselines, one-command runs
  eval/                  LLM evaluation harness, prompt sets, results
  export/                scripts to export and quantize the Llama models
  content/               skill graphs, misconception-tagged question bank, notes
  bench/                 benchmark scripts, raw results, device table
  docs/                  architecture diagrams, report, demo script
  releases/              APK and model checksum file
```

### 17.2 Technical report outline

1. Problem and users
2. System overview and architecture diagram
3. Why deterministic engine plus on-device Llama
4. Model export, quantization and ExecuTorch integration on Arm
5. Learning engine: knowledge tracing, misconceptions, planner
6. Innovation features and their status table (Shipped, Prototype, Designed)
7. Performance, energy and thermal results, compared with the mentors' reference numbers
8. Simulator and LLM evaluation results with ablations
9. Pilot findings and device coverage table
10. Privacy and offline design
11. Limitations, honest failures and what we cut
12. Phase 2 plan (on-device LoRA, more domains, more languages, NPU path)
13. References, licences and credits

### 17.3 Demo video (4 to 5 minutes)

| Time | Content |
| --- | --- |
| 0:00 to 0:30 | Problem, target user, and the offline privacy promise |
| 0:30 to 2:30 | Live airplane-mode flow from the demo path in section 13.3 |
| 2:30 to 3:30 | Innovation tour: hint ladder, syllabus scan, governor panel |
| 3:30 to 4:30 | Numbers: benchmark table, thermal chart, simulator results |
| 4:30 to 5:00 | Impact, limitations, and Phase 2 |

Record screen and phone footage on the main device, with captions in English and Hindi.

### 17.4 Final checklist

- [ ] Clean-clone build works and the APK installs on a fresh phone
- [ ] Airplane-mode test passes; manifest check passes
- [ ] All unit tests and the simulator reproduce the report numbers with one command
- [ ] Benchmark table filled for at least 8 devices
- [ ] README, licences and credits complete; model checksums listed
- [ ] Demo video uploaded and linked in the README
- [ ] Technical report as a PDF in the repo
- [ ] Release tag and backup APK link created
- [ ] Submission form filled and a confirmation saved (target 22 Nov)
- [ ] Faculty mentor and team details match the registration

## 18. Open questions and assumptions to verify

These are the facts we do not yet know or that came from memory rather than the challenge materials; each has an owner and should be closed in the first three days.

- [ ] **Team size and phones.** How many registered members write code, and which Arm Android phones (model and RAM) do they own? This decides the model variant and benchmark plan.
- [ ] **Helper rules.** Confirm by email to support@armbharatchallenge.com that testers, reviewers and pilot learners outside the 3 registered members are acceptable.
- [ ] **Submission form.** The organisers said the form link would arrive in the first or second week of November; check the inbox and the challenge site daily.
- [ ] **Doubt-clearing session.** Attend it and bring the questions below.
- [ ] **Llama 3.2 language support.** Confirm on the model card which languages are officially supported before promising Hindi generation quality.
- [ ] **ExecuTorch features.** Ask the mentors (tech-queries@armbharatchallenge.com, subject starting "PS4") about prefix and KV-cache reuse, structured decoding, NPU delegates on common phones, and any on-device fine-tuning path.
- [ ] **Quantization recipe.** Confirm the current recommended INT4 export recipe and the availability of KleidiAI kernels in the ExecuTorch version we use.
- [ ] **Licensing of data.** Confirm licences for any question-bank sources, datasets, OCR libraries and for training data used in the offline LoRA.
- [ ] **Energy measurement method.** Decide between battery counters and a USB power meter, and whether anyone has one.
- [ ] **Domains.** Choose the two polished domains (suggested: DSA for placements and ML engineering) and name a subject expert reviewer for each.
- [ ] **Faculty mentor.** Confirm the mentor and ask for a review of the question bank and report.

**Assumptions used in this PRD:** the mentor reference numbers (22+ tokens/sec, under 1.8 GB RAM) are targets rather than guarantees; the Phase 1 evaluation criteria are the four on the mentoring slides; and NPU acceleration is a stretch, with the Arm CPU as the primary path.
