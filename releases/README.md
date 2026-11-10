# Releases

Model files are never committed to Git. A release carries the APK and the model:

1. Build the model: `python export/build_pte.py ...` (writes `releases/CHECKSUMS.txt`; commit that file, not the `.pte`).
2. Create a GitHub release and attach `llama3_2_1b_int4.pte`, `tokenizer.model` and `LICENSES/LLAMA-3.2-LICENSE.txt`
   (`gh release create model-v1 build/llama3_2_1b_int4.pte tokenizer.model LICENSES/LLAMA-3.2-LICENSE.txt --notes "..."`).
   If the release is too large for the account, document another transfer method here (shared drive link, `adb push`).
3. Anyone can verify a download: `python scripts/verify_model.py path/to/llama3_2_1b_int4.pte`.
4. The app bundles `CHECKSUMS.txt` as `model_checksums.txt` and checks size and SHA-256 before every load (`ModelVerifier`); a model that
   is not listed or does not match is refused, so a corrupted or swapped file cannot run.

Model transfer used for beta testers (A19, status): FILL IN (release asset URL or documented method).
