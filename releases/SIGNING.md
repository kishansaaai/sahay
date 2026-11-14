# Signed release candidate (B21)

`./gradlew :app:assembleRelease` produces an **unsigned** APK until the team supplies all four signing variables:
`SAHAY_KEYSTORE` (absolute path), `SAHAY_KEYSTORE_PASSWORD`, `SAHAY_KEY_ALIAS`, `SAHAY_KEY_PASSWORD`.
The keystore, passwords and model weights must never be committed. Reuse the team's release key for future updates.

For a candidate with ExecuTorch, add `-Psahay.executorch=true`. Default builds use the template runtime.
The workflow `signed-release-candidate` uses the protected GitHub environment `release`, with secrets
`SAHAY_KEYSTORE_BASE64`, `SAHAY_KEYSTORE_PASSWORD`, `SAHAY_KEY_ALIAS`, `SAHAY_KEY_PASSWORD`, and uploads an APK plus SHA-256.
It does not publish a release or claim device validation.

Before tagging or distributing:

1. `apksigner verify --verbose --print-certs app-release.apk` and record the signing certificate SHA-256.
2. Verify the final APK has no INTERNET permission (`apkanalyzer manifest permissions app-release.apk`).
3. Follow `docs/TESTING.md` on a clean physical Arm phone in airplane mode; include a process-death and data-deletion check.
4. Obtain the team review required by CONTRIBUTING.md. Create a real version tag and release with APK, checksum and installation notes.
5. Add the release URL and backup APK URL to README only when they exist. Attach separately verified model assets according to Stream A's procedure.

No signing identity, release tag, clean-phone result or backup URL has been invented for this change.
