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

## Tester handoff steps (Stream B)

Download the `sahay-debug-apk` artifact from the PR's Android CI run and verify its included SHA-256 before installation.
The default debug build uses template text; real model tests need the separately verified ExecuTorch build and weights from Stream A.

1. Record the phone in `bench/devices.md`. Keep airplane mode enabled for the functional pass.
2. Cold install, launch, change language, complete intake and diagnostic. Rotate during intake and during the quiz.
3. Submit a wrong answer, inspect the roadmap reason and active misconception, then use recovery with each budget.
4. Force-stop and relaunch. Confirm answers, confidence calibration, profile and plan survive exactly.
5. Export to JSON; import it; compare answer counts and mastery. Try malformed JSON and confirm data is retained.
6. Open Scan. Photograph a Latin or Devanagari syllabus, correct OCR text, generate a graph or practice question.
   Time the entire scan-to-quiz path. Generated questions are labelled drafts and do not change mastery.
7. Open QR plans. Scan every page on another phone in shuffled order. Check the imported full snapshot, then add
   five synthetic learners under pseudonyms to the class heatmap. Rescanning a learner must update, not duplicate.
8. On Android 12+, allow the microphone and try on-device voice recognition. Try a downloaded offline TTS voice.
   Missing support must show an error and preserve typing. Record voice results on at least three phones; it remains Prototype until then.
9. Delete all data. Relaunch and confirm no learner snapshot, downloaded model, cached photo or class view survives.

## Accessibility evidence (B20)

For every screen and dialog, record pass/fail, phone, font scale and theme. Code changes provide radio-button selection
semantics, a labelled confidence slider, at least 48dp answer targets and wrapping action rows. They do not replace these checks.

- TalkBack: heading/order, selected answer announcement, sliders, text fields, language chips, source note, Stop and permission denial.
- Maximum system font size and 200% display/font scaling: portrait and landscape; every action remains reachable by scrolling.
- Light/dark themes and Android high-contrast text: inspect normal/disabled/error text and the mastery indicators.
- Switch access / external keyboard: traversal order, activated buttons, focus on dialogs and back navigation.

## Hindi review

Two native speakers review intake, question feedback, errors, export/import and all new flows. Record reviewer ID, APK SHA,
incorrect copy, suggested correction and whether the model or a template produced each reply. Additional locale packs currently
translate core navigation; detailed strings intentionally fall back to English and still need a complete native-language review.

Report failures with the issue ID, exact reproduction steps and a screenshot or log. Do not report synthetic unit tests as phone measurements.
