# Installing a Sahay debug build (B16)

The `android` job in `.github/workflows/ci.yml` builds `app-debug.apk` on every push, every pull request and on a daily schedule (02:30 UTC). It needs no secrets: the debug build is signed with the standard Android debug keystore that Gradle generates on the runner, so the APK installs on a phone without extra signing setup.

This APK is for testing only. It is not the release artefact (B21/#40) and must not be published as one.

## Get the APK

1. Open the repository's **Actions** tab and pick the workflow run you want (the newest run on `main` for the daily build).
2. Scroll to **Artifacts** and download `sahay-debug-apk`.
3. Unzip it. The archive contains `app-debug.apk`, `apk-sha256.txt` and, on current builds, `signing-cert.txt`.

To force a fresh build without pushing a commit, run the workflow manually:

```
gh workflow run ci.yml --ref main
gh run list --workflow ci.yml --limit 1
```

The daily run and a manual run produce the same artefact; there is no "integration" branch.

## Install

Over USB with the Android platform tools:

```
adb install -r app-debug.apk
```

Or copy the APK to the phone and open it. Android will warn that the app comes from an unknown source; allow it for the file manager you used. Debug builds also trigger Play Protect warnings on some phones; that is expected for a debug-signed APK.

The voice prototype requests microphone access only when you choose Speak. There is no INTERNET permission, including after dependency manifest merging. The system camera handles capture without granting this app camera permission.

## What to check on the phone

Nothing in this list has been verified yet by the team. Treat it as a test script, not a report.

- Install and launch; the welcome screen appears and the bottom bar has Today, Dashboard and Settings.
- Finish intake with a free-text goal; a roadmap appears with week cards and reasons.
- Answer a diagnostic question; the mastery bar on Today moves and the number of attempts goes up.
- Open Settings: the model row says the model file is not available (no `.pte` is bundled) and the tutor/explanation text is therefore labelled as template output.
- Copy an export, then import it back; the learner totals should be identical.
- Delete all data; the app returns to the welcome screen and the snapshot file is gone.
- Open Device and energy: readings appear after a few seconds; the panel says the thresholds are untuned defaults.
- Rotate the phone with a plan loaded; state should survive.
- Turn on airplane mode; everything above should still work.

## Reporting

Record the phone model, Android version, the APK's SHA-256 from `apk-sha256.txt`, and the exact steps. Use the device table in `bench/` for timings and battery numbers. Numbers measured on one phone must not be presented as results on another.

For scan, QR/classroom, voice, large-text and Hindi test cases, use [BETA.md](BETA.md). Release candidates have a separate [signing procedure](../releases/SIGNING.md).

A verified current-main APK and its checksum/signature evidence are linked in [ANDROID_ARTIFACT.md](ANDROID_ARTIFACT.md).

The CI debug signing key is reused through a test-only Actions cache so normal daily builds can update each other.
The production release key remains separate and must come from the team signing secrets. Cache resets or older debug builds
can have a different certificate: the phone collector reports an incompatible update and does not uninstall the existing app.
For the Vivo V21e handoff and automatic hardware/OS/RAM collection, see [VIVO_TESTING.md](VIVO_TESTING.md).
