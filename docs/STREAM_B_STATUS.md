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
| [B14 #33](https://github.com/kishansaaai/sahay/issues/33) | Device/comparison register in bench/devices.md; actual-phone metadata/OS/RAM collector in scripts/check_phone.py; Vivo handoff in docs/VIVO_TESTING.md; native thread API limitations documented | Real native thread sweep and 1B INT4 / 1B INT8 / 3B INT4 speed/RAM/quality runs; needs Stream A exports and runner support |
| [B15 #34](https://github.com/kishansaaai/sahay/issues/34) | Beta procedure, device register, Hindi/accessibility review steps and a Vivo V21e collector that records facts without guessing variant/specs | Eight distinct Arm phones and native reviewers; no synthetic device results substituted |
| [B16 #35](https://github.com/kishansaaai/sahay/issues/35) | Gradle wrapper; APK/checksum artifacts; daily/manual builds retained; Room/localization/lint/merged-manifest/pinned-runtime compile checks added | Main CI APK, checksum and debug signature verified on 2026-11-15; physical install remains part of B21/beta |
| [B17 #36](https://github.com/kishansaaai/sahay/issues/36) | Checksummed multipart QR, complete snapshot round trip, camera/image scanning, explicit import, pseudonymous teacher heatmap; existing SAHAY1 text/QR compatibility and batch class averages retained; tests with five simulated learners | Physical camera/display transfer and teacher usability review |
| [B18 #37](https://github.com/kishansaaai/sahay/issues/37) | On-device STT (Android 12+) and downloaded non-network TTS; explicit permission/error/typing fallback; labelled Prototype | Airplane-mode voice checks on at least three phones |
| [B19 #38](https://github.com/kishansaaai/sahay/issues/38) | 303 UI resource keys in all six languages; CI rejects missing translations and mismatched placeholders; runtime locale switch preserves the in-progress diagnostic and persists selection; experimental generated-language notice retained | Native-language copy/quality review; course notes and template/generated text have a separate language review |
| [B20 #39](https://github.com/kishansaaai/sahay/issues/39) | Selected-answer radio semantics, labelled confidence/text inputs, 48dp answer targets, wrapping actions, single-line app-bar/tab labels; automated 200% portrait/landscape renders and light/dark text-contrast checks | TalkBack, maximum font/display scale and contrast verification on physical devices |
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

Lint fails on missing translations. All six UI packs must have the same keys and format placeholders; generated content and source notes are reviewed separately.
An unsigned release build is compilation evidence, not a distributable signed release.


## Local verification result (2026-11-14)

- 69 Kotlin engine/runtime/transport checks passed; Python and Kotlin engine/prompt parity passed in GitHub CI.
- Six Robolectric tests passed: Room transactions/migration/deletion, QR bitmap encode/decode, bounded image loading,
  and the live intake/diagnostic/practice/import/restart/delete state loop.
- LLM reference tests passed (26); repository/content and locale-placeholder checks passed.
- Debug APK and unsigned release APK built successfully. The opt-in ExecuTorch v0.7.0 integration compiled successfully.
- Both debug and release merged manifests passed the no-INTERNET check.
- Debug lint passed with zero errors. Warnings remain, including the deliberately partial additional-language packs.

These are automated/software checks. The physical-phone and human-review acceptance items in the table remain open.


## Follow-up verification (2026-11-15)

PR #73 was merged and main CI passed for `12abdbada0cf1ade437f08556457cc86d3d8a28a`.
This follow-up completes the four additional UI packs (303 keys each), localizes app-generated status/count messages,
and verifies locale changes without resetting a pending answer or confidence value. Native-language review remains pending.

The B20 suite renders settings in all six languages at 200% text scale in landscape, checks selected-answer and slider
semantics in portrait, and exercises bottom-tab navigation with long labels in dark mode. It also checks WCAG AA normal-text
contrast (at least 4.5:1) for eight foreground/background pairs in both themes. Generated PNGs are uploaded as
`sahay-accessibility-previews`. These checks are headless Android evidence, not a physical TalkBack result.

The current-main debug artifact was downloaded and independently verified; see [ANDROID_ARTIFACT.md](ANDROID_ARTIFACT.md).


The user identified a Vivo V21e as the prospective test phone. The read-only ADB collector records actual installed SDK/RAM/SoC fields,
with an opt-in update attempt that never uninstalls or clears data. It correctly stops when no authorized phone is attached. No phone
facts or performance numbers were produced here. Debug CI also reuses a test-only signing cache and exports certificate evidence;
release signing remains separate.


The follow-up local run passed 11 Android/JUnit tests (including the rendered UI matrix and contrast checks), two phone-evidence
parser tests, the debug APK build, lint with zero errors, full locale/placeholder checks and the merged-manifest no-INTERNET check.
The Vivo collector was also exercised with no connected device and correctly stopped without producing a device report or attempting an install.
