# Debug APK evidence (B16)

Verified on 2026-11-15 for main commit `12abdbada0cf1ade437f08556457cc86d3d8a28a`.

- [Successful main CI run](https://github.com/kishansaaai/sahay/actions/runs/43632410703)
- [Download sahay-debug-apk](https://github.com/kishansaaai/sahay/actions/runs/43632410703/artifacts/14128338744)
- Archive contains `app-debug.apk` and `apk-sha256.txt` (private-repository GitHub access is required).
- APK SHA-256: `fc43ad1d832c9a79cea68334d5fc3df8b4d93a3af3d059e11a1f37c6eebc7deb`
- `apksigner verify --verbose --print-certs` succeeded with APK Signature Scheme v2.
- Debug certificate: `C=US, O=Android, CN=Android Debug`.
- Certificate SHA-256: `758ed647fc5371a5a6b18744a22e79834d3eca0e820591d1b83f49b8fcffd1d7`.
- `aapt dump permissions` showed microphone permission and the app-scoped receiver permission; no INTERNET permission.

This verifies the CI artifact, archive checksum, signing structure and final APK permissions. It does not establish a
physical-phone installation, model latency, airplane-mode functional test or production release signature.
The default debug artifact uses the labelled template runtime. Use the Stream A model transfer procedure for real-model tests.

Installation and test steps: [TESTING.md](TESTING.md). Distribution and device records: [BETA.md](BETA.md).
