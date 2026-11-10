# LLM evaluation harness (issue A16, PRD 12.3)

One command reproduces the LLM numbers in the report:

    python eval/run_eval.py --generator-cmd "<command that reads a Llama-3 prompt on stdin and prints the completion>" --label base-1b-int4

It runs 500 fixed prompts for each of the seven tasks (`prompt_sets.py`, seed and SHA-256 per set in `MANIFEST.json`, so a changed set is caught by
`python eval/prompt_sets.py`) through the real orchestrator and writes `eval/results/<label>/summary.md|json` (JSON validity **before retry**,
**after retry**, fallback rate, latency) and `outputs.jsonl` (every output). Use `--generator mock` only to test the harness: it fabricates
output and its numbers are not model results.

| Need | Command |
| --- | --- |
| Base vs tuned (X5) | run twice with different `--label`, then `python eval/run_eval.py --compare base-1b-int4 tuned-1b-int4` |
| 100 explanations for two reviewers (correct, clear, grounded, level) | `python eval/rubric.py sample --run <label>`, reviewers fill `explanations.csv`, then `python eval/rubric.py score --a r1.csv --b r2.csv` |
| Hindi review: 50 English/Hindi pairs, two native speakers | same `sample` command writes `hindi_pairs.csv`; score with `hindi-score` |
| Teach-back agreement on 40 explanations | `sample` writes `teachback_for_human.csv`; a human fills `human_*`; `python eval/rubric.py teachback --csv <file>` (target 0.70) |
| Subset on the phone | wrap `adb shell` in the `--generator-cmd` and use `--n 50` |

`--generator-cmd` receives the prompt on stdin and `SAHAY_TEMPERATURE` / `SAHAY_MAX_TOKENS` in its environment. Example for a laptop ExecuTorch runner:
`"./cmake-out/examples/models/llama/llama_main --model_path build/llama3_2_1b_int4.pte --tokenizer_path tokenizer.model --temperature $SAHAY_TEMPERATURE --seq_len 512 --prompt \"$(cat)\""`
(check the runner's flags for your ExecuTorch tag; the runner may echo the prompt, and the orchestrator takes the first JSON object after it).

Status: harness and sets are done and tested; **no model numbers exist yet**.
