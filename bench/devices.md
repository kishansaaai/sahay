# Beta device register (B14, B15)

No physical-phone measurements have been collected in this workspace. The target is **8 distinct Arm phones**.
Use pseudonymous tester IDs; volunteers test and review the APK, and do not contribute implementation code.

| Tester ID | Phone | SoC / Arm architecture | RAM | Android | APK SHA-256 | Model SHA-256 | Date | Airplane mode | Benchmark file | Hindi review |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

For each entry, attach the raw benchmark output and record charging state, brightness, room temperature and starting thermal state.
Do not fill this table with emulator results or invent phones to meet the target.

## Comparison matrix (B14)

| Phone / conditions | Model | Actual native threads | Median TTFT | Median decode tok/s | Peak PSS MB | Fixed quality set | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |

Use 1B INT4, 1B INT8 and 3B INT4, with the same `export/prompts20.json`, tokenizer, context and generation settings.
The pinned Java `LlmModule` has no native thread-pool setter. Governor thread counts are policy targets, not measured native settings.
A real thread sweep therefore needs a runner exposing that setting or separate native builds. Record which mechanism was used;
changing a UI label does not establish a thread-count measurement. Runtime/export changes belong to Stream A.
