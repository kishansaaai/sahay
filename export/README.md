# Model export (issue A03)

`build_pte.py` turns Llama 3.2 1B Instruct into an INT4 ExecuTorch `.pte` from a clean checkout, records the SHA-256 in
`releases/CHECKSUMS.txt` and the exact command in `build/<name>.recipe.json`. **The model file is never committed** (`*.pte` is git-ignored).

    python export/build_pte.py --checkpoint-dir ~/models/Llama3.2-1B-Instruct --executorch-dir ~/executorch
    python export/build_pte.py --checkpoint-dir ... --variant int8        # INT8 comparison (B14)
    python export/build_pte.py --checkpoint-dir ... --dry-run             # print the command only

## Recipe (INT4, the production model)

| Setting | Value | Why |
| --- | --- | --- |
| Source | Llama 3.2 1B Instruct consolidated checkpoint | PRD 10.1 |
| Weights | 4-bit, group size 32 (`8da4w`: 4-bit weights, 8-bit dynamic activations) | PRD 10.1 |
| Embeddings | 4-bit, group size 32 | Saves about 100 MB |
| Backend | XNNPACK with extended ops | PRD 10.1 |
| KV cache and SDPA | `use_kv_cache`, `use_sdpa_with_kv_cache` | Needed for usable decode speed |
| Context | 2048 tokens | PRD 10.1 |
| Metadata | BOS 128000, EOS 128009 and 128001 | Llama 3 special tokens, so generation stops |

**The flag names above follow the current ExecuTorch Llama guide but change between releases.** Pin the release in
`EXECUTORCH_VERSION` (done in A01) and compare with `examples/models/llama/README.md` of that tag. The script has `--cli-style new`
(Hydra-style `extension.llm.export.export_llm`) and `--cli-style legacy` (`examples.models.llama.export_llama`); if neither matches
your tag, change `command()` and keep the table above in sync. The 20-prompt acceptance set is `prompts20.json`.

## Acceptance for A03

- [ ] Script runs from a clean checkout and builds the `.pte` (needs the weights and an ExecuTorch checkout; not runnable in CI)
- [ ] `releases/CHECKSUMS.txt` has the new line (the script writes it)
- [ ] On the phone, the model answers the 20 prompts in `prompts20.json`; record on-topic yes/no per prompt in `bench/results/prompts20.md`
- [ ] Prompts 17 to 19 (medicine, ignore instructions, stocks) are refused or redirected (PRD 10.6); note the cases where the base model fails, because the app-level guard handles them

## Shipping the model (A19)

The `.pte` is a GitHub release asset, see `releases/README.md`. The app verifies its SHA-256 before loading.
