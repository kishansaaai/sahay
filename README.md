# Sahay: on-device adaptive learning companion

Bharat AI-SoC Challenge 2nd Edition, Problem Statement 4: **On-device personalised learning assistant**.

An Android app that builds and re-plans a personal study roadmap using a quantized Llama 3.2 model running fully on the phone through ExecuTorch, with no internet permission.

- Product spec: [docs/PRD.md](docs/PRD.md)
- Sprint plan and owners: [SPRINT.md](SPRINT.md)
- Backlog source: [docs/backlog.json](docs/backlog.json)

## Repository layout

| Folder | Purpose |
| --- | --- |
| app/ | Android app (Kotlin, Compose, JNI to ExecuTorch) |
| engine/ | Learning engine (Kotlin module, no Android dependency) |
| simulator/ | Learner simulator and baselines |
| eval/ | LLM evaluation harness, prompt sets, results |
| export/ | Scripts to export and quantize the Llama models |
| content/ | Skill graphs, misconception-tagged questions, prompts |
| bench/ | Benchmark scripts, raw results, device table |
| docs/ | PRD, architecture, report, demo script |
| releases/ | APK notes and model checksums (model files are never committed) |
| LICENSES/ | Llama licence notice, library and dataset credits |

## Feature status (update as you go)

| ID | Feature | Status |
| --- | --- | --- |
| B1-B4 | ExecuTorch + INT4 Llama, JNI streaming, no INTERNET permission | In progress (export script, runtime wrapper, model manager, checksum check written and unit-tested; nothing verified on a phone yet; B2 manifest check is in CI) |
| C1-C7 | Intake, skill graph, diagnostic, tracing, planner, tutor, dashboard | Not started |
| I1-I8 | Hint ladder, governor, simulator, syllabus compiler, analogies, recovery, teach-back, scan | In progress (LLM side of I1, I2 policy, I4 validator, I5, I7 grading written and unit-tested; needs device and model runs) |
| X1-X5 | Pacing, voice, QR sharing, KV-cache, offline LoRA | Not started |

## Rules

- Original work of the registered team only; credit every dataset, library and model in LICENSES/.
- Never commit model weights (`*.pte`, checkpoints); use the export script and release assets.
- No analytics SDKs and no INTERNET permission (CI enforces the manifest check).
