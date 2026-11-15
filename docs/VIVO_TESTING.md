# Vivo V21e device handoff (B14, B15, B18)

The user identified the test phone as **Vivo V21e** on 2026-11-15. Its exact variant, installed Android version, SoC and RAM
have not been read from the phone. No ADB device is attached to the cloud workspace. Published launch specifications do not
establish which OS update or language packs are installed, so the collector reads these facts from the actual handset.

## Read the details on the phone

Download `sahay-vivo-test-kit` from the follow-up PR's green CI run, unzip it and install `app-debug.apk` on the phone.
Open Sahay → **Device and energy** on the welcome screen → **Copy phone details**. The copied JSON reports actual Android/SDK,
platform/CPU ABIs, kernel-managed RAM and on-device speech-service detection without serial or learner data. No intake or diagnostic
is required to reach this panel. A detected service still needs the requested language pack before voice can work.

The kit contains a README, APK checksum/certificate evidence and optional Windows/Linux/macOS collector launchers.
No model weights or signing private keys are packaged. It is a debug build with labelled template replies.

## Connect and collect

1. Download and unzip `sahay-debug-apk` from the follow-up PR's green CI run. Keep its checksum and certificate files beside the APK.
2. Install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) on your computer and add `adb` to PATH.
3. Enable Developer options and USB debugging on the phone. Connect by USB, unlock it and approve the computer's debugging prompt.
4. `adb devices` must show one authorized device. The script refuses to choose among multiple phones without `--serial`.
5. With Python 3, run this from a checkout or the supplied device kit:

```
python scripts/check_phone.py --label vivo-v21e --apk path/to/app-debug.apk --out phone-evidence
```

In the supplied kit, use `check_phone.py` instead of `scripts/check_phone.py`, or use its collector launcher.
The first command reads facts and records the APK hash; it does not install anything. To attempt an update:

```
python scripts/check_phone.py --label vivo-v21e --apk path/to/app-debug.apk --install --out phone-evidence
```

Optionally add `--expected-sha256` with the first field from `apk-sha256.txt`; a mismatch stops before installation.
On Windows, use `py -3` instead of `python` if that is your Python launcher. If adb is not on PATH, add `--adb path/to/adb.exe`.

The JSON records the model identifier, OS/SDK/security patch, available SoC/platform properties, CPU ABIs, kernel-managed RAM,
airplane-mode state, APK SHA-256 and any requested install result. Unknown fields stay null. It does not collect account details,
learner data or device serials. Kernel-managed RAM is labelled separately from marketed or virtual/extended RAM.

An incompatible-signature update is reported without uninstalling the existing app. Export learner data from Settings before
choosing any manual clean installation. The collector never resets, clears or deletes phone data.

## Vivo acceptance sequence

- Use USB so ADB remains connected in airplane mode. Turn airplane mode on using the phone UI, then rerun the collector to record it.
- Launch the app. Complete intake, diagnostic, a wrong answer, roadmap, a practice answer and recovery. Rotate and switch languages
  without losing the pending choice/confidence. Check the largest system text size and both light/dark modes.
- Force-stop and reopen. Verify attempts/mastery and export/import. Follow the data-deletion case from `docs/BETA.md` after saving the export.
- Check OCR preview/edit with a Latin or Devanagari page, and transfer all QR pages to a second phone. Record scan-to-quiz time.
- If the installed SDK is below 31 (Android 12), this app cannot use the strict on-device STT API: record it as unsupported and keep
  voice marked Prototype. Typing and a downloaded non-network TTS voice can still be tested. SDK 31+ still needs an on-device recognizer
  and language pack; SDK support alone is not proof of availability.
- The default debug APK uses labelled template text. Real-model latency, tokens/s, memory, energy and comparison runs require Stream A's
  separately verified model/runtime setup. Use `bench/README.md`, `bench/ENERGY.md` and `bench/devices.md` for those measurements.

The Vivo is one prospective device, not eight-phone coverage. This procedure and synthetic script tests are not measurements.
