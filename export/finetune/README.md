# Offline LoRA fine-tune (issue A18, feature X5, PRD 10.7)

Status: **tooling only. No adapter has been trained.** The ship rule stands: the base model ships unless the tuned one is better.

## Steps

1. **Data.** Build `sft.jsonl` of reviewed examples in Hindi and English (tutoring tone, hint ladders, strict JSON for all seven tasks). Each line carries `source`, `licence` and `reviewed_by`; list every source in `DATA_SOURCES.md` and in `LICENSES/README.md`. Only data licensed for training may be used: our own notes and hand-written examples are fine; outputs of other commercial models usually are not, so check their terms first.
2. **Check it.** `python export/finetune/validate_sft.py sft.jsonl` rejects invalid JSON, schema violations, hint examples that leak the answer, duplicates and lines with no source, licence or reviewer.
3. **Train** on Colab: `python export/finetune/train_lora.py --data sft.jsonl --out lora-out` (unrun; adjust versions as needed).
4. **Merge and export.** Merge the adapter into the base weights (`peft` `merge_and_unload()`), save in the format `export/build_pte.py` expects (convert to the Meta consolidated format if your ExecuTorch tag needs it), then run `build_pte.py` with a different `--out` name so the base `.pte` is untouched. The Llama licence applies to the tuned variant (LICENSES/LLAMA-3.2-NOTICE.md).
5. **Evaluate.** `python eval/run_eval.py --generator-cmd "<runner for tuned>" --label tuned-1b-int4`, same for `base-1b-int4`, then `python eval/run_eval.py --compare base-1b-int4 tuned-1b-int4`, plus the explanation rubric (`eval/rubric.py`). Ship tuned only if it wins on both validity and rubric score.

## Result (FILL IN)

Tuned vs base table, decision (ship base / ship tuned), and the reason.
