# Stream B implementation and acceptance record

Scope: the **19 open issues assigned to Pradhyut21**, B04–B22 (#23–#41). B01–B03 are already closed.
Runtime/engine edits in this branch support these Android features; other owners' issue implementations are outside this branch.
No issue is automatically closed by this record. CONTRIBUTING.md requires another team member's review and green CI.

| Issue | Work in this branch / existing implementation checked | Acceptance still requiring evidence |
| --- | --- | --- |
| [B04 #23](https://github.com/kishansaaai/sahay/issues/23) | Room tables for content, profile, attempt log, state caches, plan/items, sessions and teach-backs; transactional replacement; ordered IO; legacy JSON migration; replay/export/import validation | Physical process-death round trip; team review |
| [B05 #24](https://github.com/kishansaaai/sahay/issues/24) | Existing intake captures goal, level, hours, deadline, interests and language with chip fallback | Render and complete on a physical phone; native Hindi review |
| [B06 #25](https://github.com/kishansaaai/sahay/issues/25) | Lossless token back pressure, cancellation ownership, stale-reply protection, governor caps, no prompt echo; source note and Stop | Measure first token under 3 seconds and Stop on the test phone with real weights |
| [B07 #26](https://github.com/kishansaaai/sahay/issues/26) | Confidence/time/hint persistence; duplicate-submit guards; old hint responses ignored; labelled slider and selectable answers | Complete diagnostic on a phone, verify stored answers after restart |
| [B08 #27](https://github.com/kishansaaai/sahay/issues/27) | Existing weekly roadmap/reasons; imported plan references are validated before use | Long roadmap and why-this checks in portrait/landscape |
| [B09 #28](https://github.com/kishansaaai/sahay/issues/28) | Today action; explanation text streams as a provisional draft, then schema validation/retry/fallback selects the final reply; Stop, analogy, check question and re-plan | Real-model grounding/analogy quality and on-phone flow |
| [B10 #29](https://github.com/kishansaaai/sahay/issues/29) | Active misconceptions added; restart preserves the distinction between rated diagnostic confidence and unrated practice; mastery/forecast existing | Measure update under 1 second; portrait/landscape and real history |
| [B11 #30](https://github.com/kishansaaai/sahay/issues/30) | Existing engine recovery and before/after/dropped reasons retained | Phone walkthrough and weekly budget evidence |
| [B12 #31](https://github.com/kishansaaai/sahay/issues/31) | Bundled Latin/Devanagari OCR, full-resolution camera/image input, edit-before-generation, validated graph/quiz drafts; no network permission | Airplane-mode capture and scan-to-quiz under 60 seconds on main phone |
| [B13 #32](https://github.com/kishansaaai/sahay/issues/32) | Periodic signals, generation speed, charge-counter estimate, governor reason log; transactional import/export and ordered data/model deletion | Real-phone energy counters, deletion and export/import; thread/context values remain policy targets until native support exists |
| [B14 #33](https://github.com/kishansaaai/sahay/issues/33) | Device/comparison register in bench/devices.md; limitations of the native thread API documented | Real native thread sweep and 1B INT4 / 1B INT8 / 3B INT4 speed/RAM/quality runs; needs Stream A exports and runner support |
| [B15 #34](https://github.com/kishansaaai/sahay/issues/34) | Beta procedure, device register, Hindi/accessibility review steps | Eight distinct Arm phones and native reviewers; no synthetic device results substituted |
| [B16 #35](https://github.com/kishansaaai/sahay/issues/35) | Gradle wrapper; APK/checksum artifacts; daily/manual builds retained; Room/localization/lint/merged-manifest/pinned-runtime compile checks added | Successful branch CI artifact and tester installation |
| [B17 #36](https://github.com/kishansaaai/sahay/issues/36) | Checksummed multipart QR, complete snapshot round trip, camera/image scanning, explicit import, pseudonymous teacher heatmap; tests with five simulated learners | Physical camera/display transfer and teacher usability review |
| [B18 #37](https://github.com/kishansaaai/sahay/issues/37) | On-device STT (Android 12+) and downloaded non-network TTS; explicit permission/error/typing fallback; labelled Prototype | Airplane-mode voice checks on at least three phones |
| [B19 #38](https://github.com/kishansaaai/sahay/issues/38) | 259 English/Hindi UI resource keys; 59 core keys each in Tamil/Telugu/Bengali/Marathi; runtime language switch; experimental model languages; intentional English fallback disclosed and checked | Native review; detailed additional-locale copy remains partial (200 fallback keys each) |
| [B20 #39](https://github.com/kishansaaai/sahay/issues/39) | Selected-answer radio semantics, labelled confidence/text inputs, 48dp answer targets, wrapping action rows; accessibility test script | TalkBack, maximum font/display scale and contrast verification on physical devices |
| [B21 #40](https://github.com/kishansaaai/sahay/issues/40) | Environment-based signing and protected release-candidate workflow; unsigned release can be compile-checked locally; signing guide | Team signing secrets, fresh-phone airplane-mode test, review, version tag/release and backup link |
| [B22 #41](https://github.com/kishansaaai/sahay/issues/41) | 4:30 recording script with English/Hindi SRT caption drafts | Record real phone/model, align/review captions, upload and add real README link |

## Verification

Run repository/content checks and locale checks:

```
python scripts/ci_checks.py
python scripts/validate_content.py
python scripts/check_locales.py
```

With Java 17 and Android SDK 35:

```
./gradlew :engine:engineTests :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease
./gradlew :app:compileDebugKotlin -Psahay.executorch=true
python scripts/ci_checks.py --merged-manifest app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
```

Lint keeps intentional additional-language fallbacks as warnings. It continues to fail on other errors.
An unsigned release build is compilation evidence, not a distributable signed release.
