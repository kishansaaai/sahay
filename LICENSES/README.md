# Licences and credits

Every library, model and data source we use is listed here **when it is introduced** (CONTRIBUTING.md, definition of done).
Full licence texts of anything we redistribute live next to this file.

## Model

| Item | Licence | What we must do | Status |
| --- | --- | --- | --- |
| Llama 3.2 1B Instruct (and 3B for the comparison) from Meta | Llama 3.2 Community License and Acceptable Use Policy | See [LLAMA-3.2-NOTICE.md](LLAMA-3.2-NOTICE.md): show "Built with Llama" in the app and report, ship the licence text with every model file, carry the attribution notice, keep to the Acceptable Use Policy. Applies to any LoRA-tuned or quantized variant we ship. | Notice written. **Model access must be confirmed by the team member who accepted the licence (tick in A02).** |

## Runtime and libraries

| Item | Licence | Where used |
| --- | --- | --- |
| ExecuTorch (PyTorch) | BSD-style licence | On-device runtime and export flow (A01, A03, A04). Confirm text against the pinned tag. |
| XNNPACK | BSD-style licence | CPU backend inside ExecuTorch |
| KleidiAI (Arm) | Apache-2.0 | Arm-optimised kernels, if the build enables them |
| Official ExecuTorch Llama Android demo app | BSD-style licence (same repository) | Reference for the JNI wrapper (A01, A04); our wrapper is original code written against its public API |
| Kotlin, kotlinx.coroutines, AndroidX, Jetpack Compose | Apache-2.0 | App and engine |
| ZXing core (`com.google.zxing:core`) | Apache-2.0 | QR symbol generation and image decoding for plan sharing (B17/X3). Library only; no source copied into the repository. |
| ML Kit text recognition, bundled models (`com.google.mlkit:text-recognition`, `text-recognition-devanagari`) | Google ML Kit Terms of Service (not an open-source licence); Android client libraries are distributed under Google's terms | On-device OCR for the Scan screen (B12, I4, I8). Models ship inside the APK, no download. **Team decision needed (PRD open item on OCR licences):** the SDK includes usage logging that cannot reach Google because the app has no INTERNET permission (stripped in the manifest, checked on the merged manifest in CI), but confirm this satisfies the "no analytics SDKs" rule, or swap `OcrEngine` for Tesseract (Apache-2.0), which is the only file that touches ML Kit. |
| Python 3 standard library only | PSF | llm/, eval/, export/, bench/ scripts (no third-party packages needed to run the tests) |
| Hugging Face transformers, peft, datasets (offline LoRA, A18, optional) | Apache-2.0 | export/finetune/ only; never shipped in the app |

## Content and data

| Item | Licence | Notes |
| --- | --- | --- |
| Skill graphs, reference notes, questions, prompts in content/ | Original work of the team | Reference notes are our own writing; resource titles are citations, not copied text |
| LoRA training data (A18) | Must be licensed for this use | Record each source and licence in export/finetune/DATA_SOURCES.md before training |
| Syllabi used to test the syllabus compiler (A12) | Check each source | Record source and permission in eval/SYLLABI.md |

## Credits to keep current

Add a row above the moment a new dependency, dataset, font, icon or code snippet enters the repository.
