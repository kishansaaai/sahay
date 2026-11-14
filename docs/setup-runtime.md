# Runtime setup and the official ExecuTorch Llama demo (issue A01)

Goal: the official ExecuTorch Llama Android demo installs and generates text on the team test phone. This is Gate 1.
**Status: not done. It needs the physical phone; fill in the sections marked FILL IN as you go and commit this file.**

## Test phone (FILL IN)

| Field | Value |
| --- | --- |
| Phone model | |
| SoC | |
| RAM | |
| Android version | |
| Free storage | |
| Date and who ran it | |

## Steps

1. Accept the Llama 3.2 licence (see LICENSES/LLAMA-3.2-NOTICE.md) and download the Llama 3.2 1B Instruct checkpoint (`consolidated.00.pth`, `params.json`, `tokenizer.model`).
2. Check out ExecuTorch at a release tag and record it in `export/EXECUTORCH_VERSION` (use the newest stable tag whose `examples/demo-apps/android/LlamaDemo` README works; write the tag here: FILL IN).
3. Set up the ExecuTorch Python environment as in its docs (`./install_executorch.sh` in a fresh virtualenv) and Android SDK / NDK as the demo README lists.
4. Build the demo's native library and APK following `examples/demo-apps/android/LlamaDemo/docs/delegates/xnnpack_README.md` in the checkout (this is the page to follow: the commands change between tags).
5. Export a `.pte` with `python export/build_pte.py --checkpoint-dir <dir>` (A03) or use the one the demo README exports.
6. `adb push` the `.pte` and `tokenizer.model` to `/data/local/tmp/llama/`, install the APK, open the demo, choose the files, send a prompt.
7. Write down: time the model took to load, whether text streamed, tokens per second the demo shows, and any crash or error text.

## Result (FILL IN)

- Installed and generated text: yes / no
- Exact commands that worked (copy from shell history):
- Observed speed and memory:
- Problems and how they were fixed:

## Blockers

If anything blocks you, email tech-queries@armbharatchallenge.com **the same day** with a subject starting with `PS4` and include phone model, ExecuTorch tag, the failing command and the last 30 log lines. Paste the sent date here: ____

## Using our own app with the runtime (A04, after A01)

The wrapper `app/src/executorch/java/.../ExecuTorchRuntime.kt` is compiled only when you opt in:

    ./gradlew :app:assembleDebug -Psahay.executorch=true -Psahay.executorchVersion=<tag version>

Put the model and tokenizer in the app's private folder:

    adb push build/llama3_2_1b_int4.pte /data/local/tmp/
    adb push tokenizer.model /data/local/tmp/
    adb shell run-as com.sahay.app mkdir -p files/models
    adb shell run-as com.sahay.app cp /data/local/tmp/llama3_2_1b_int4.pte /data/local/tmp/tokenizer.model files/models/

The app refuses to load a model whose SHA-256 is not in `releases/CHECKSUMS.txt`. Class and constructor names of `LlmModule` change between ExecuTorch tags: compare the wrapper with the demo's `LlamaDemo` sources for your tag and adjust `ExecuTorchRuntime.kt` if it does not compile.
