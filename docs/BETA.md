# Beta test handoff (B15, B20)

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
