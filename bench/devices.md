# Device table (B15)

One row per tested phone. No rows have been filled yet: the app has not been installed on a physical device.

| Phone | SoC | RAM | Android | APK SHA-256 | Installed | First launch | Diagnostic q / min | Output | Airplane mode | Crashes / ANRs | Hindi review |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| _none yet_ | | | | | | | | | | | |

Rules:

- The APK SHA-256 must match `apk-sha256.txt` from the CI artifact.
- "Output" is `template` when no `.pte` was present, otherwise the model file name and its SHA-256 from `releases/CHECKSUMS.txt`.
- Diagnostic minutes come from the app, not from a stopwatch around the whole session.
- Do not average across phones in this file; the report does that, with the count stated.

## Thread / quantisation comparison (B14, from Stream B)

| Phone / conditions | Model | Actual native threads | Median TTFT | Median decode tok/s | Peak PSS MB | Fixed quality set | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |

Use 1B INT4, 1B INT8 and 3B INT4, with the same `export/prompts20.json`, tokenizer, context and generation settings.
The pinned Java `LlmModule` has no native thread-pool setter. Governor thread counts are policy targets, not measured native settings.
A real thread sweep therefore needs a runner exposing that setting or separate native builds. Record which mechanism was used;
changing a UI label does not establish a thread-count measurement. Runtime/export changes belong to Stream A.
