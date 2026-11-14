# Beta programme kit (B15)

Goal: at least eight distinct Arm phones tested against the debug APK, with a filled device table, a Hindi copy review and no volunteer writing code. None of this has happened yet; this file is the kit, not a report.

## Who can take part

- Volunteers test and report only. They do not push code, open pull requests or hold repository write access.
- Testers keep their own learner data: the app stores everything on the phone, and the export/share features are optional. Nothing is uploaded by the app.
- A tester may share an exported snapshot (clipboard text or QR) with the team for debugging, but only if they choose to.

## What to send a tester

1. The CI artifact `sahay-debug-apk` (see [TESTING.md](TESTING.md)) and its `apk-sha256.txt`.
2. The smoke script in [TESTING.md](TESTING.md).
3. The table below, or the issue link that contains it.
4. One sentence on what to do when something fails: screenshot, exact steps, phone model, Android version, APK SHA-256.

## Device table

Fill one row per phone in [bench/devices.md](../bench/devices.md). Do not merge rows for two phones of the same model if their RAM or Android version differs.

| Column | Why it is needed |
| --- | --- |
| Phone model and codename | Results are only comparable within a device |
| SoC and RAM | The governor and model loading depend on both |
| Android version and build | Thermal API behaviour changed several times |
| APK SHA-256 | Ties the row to the exact build |
| Install and first launch | The minimum "does it install" evidence |
| Diagnostic questions and minutes | The only real measurement of the C08 duration target |
| Template or model output | Whether a `.pte` was present; never mix the two in one row |
| Airplane-mode result | The offline claim, per device |
| Crashes / ANRs | Count and screen |
| Hindi review verdict | Natural, clipped, or wrong; quote the string |

## Hindi review (B19 follow-up)

For each screen, read the Hindi UI aloud and mark: natural / understandable / wrong. Record the exact string id (`values-hi/strings.xml`) for anything that needs a change. Do not rewrite strings in the review spreadsheet without a screenshot; context matters.

Screens to cover: welcome, intake, diagnostic, roadmap, today, study, tutor, dashboard, recover, settings, device, share.

## What not to claim

- A tester saying "it works" is not a benchmark. Timings and battery numbers need the protocol in `bench/`.
- One phone is not the device table. The report must say how many phones were tested and which ones.
- If fewer than eight phones are recruited, say the number; do not pad the table.
