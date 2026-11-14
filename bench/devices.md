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
