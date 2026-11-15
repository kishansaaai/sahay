#!/usr/bin/env python3
"""Package the tested debug APK and its data-safe Vivo collection handoff. No model weights or signing keys are included."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]


def build(apk, out, commit='local-build'):
    if not apk.is_file():
        raise ValueError('debug APK is missing: %s' % apk)
    out.mkdir(parents=True, exist_ok=True)
    target = out / 'app-debug.apk'
    shutil.copyfile(apk, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    (out / 'apk-sha256.txt').write_text(digest + '  app-debug.apk\n', encoding='utf-8')
    shutil.copyfile(ROOT / 'scripts/check_phone.py', out / 'check_phone.py')
    shutil.copyfile(ROOT / 'docs/VIVO_TESTING.md', out / 'VIVO_TESTING.md')
    certificate = apk.parent / 'signing-cert.txt'
    if certificate.is_file():
        shutil.copyfile(certificate, out / 'signing-cert.txt')
    (out / 'KIT.json').write_text(json.dumps({'schema': 'sahay.phone-kit/1', 'source_commit': commit,
        'apk_sha256': digest, 'build_type': 'debug-template-runtime',
        'contains_model_weights': False, 'contains_signing_keys': False}, indent=2) + '\n', encoding='utf-8')
    (out / 'README.txt').write_text(f'''Sahay Vivo V21e debug test kit

This is a test APK, not a production release. Model weights are not bundled; the default build uses labelled template replies.
UI resources cover English, Hindi, Tamil, Telugu, Bengali and Marathi. Actual phone performance is still to be measured.

On the phone (no desktop collector required):
1. Install app-debug.apk using the Android package installer.
2. Open Sahay and select Device and energy on the welcome screen.
3. Select Copy phone details. Paste that JSON into the project chat to report the actual installed OS, platform and RAM.
   It excludes serial numbers and learner data. A detected speech service does not prove every language pack is installed.

Optional desktop collection:
Install Python 3 and Android platform-tools (adb). Enable USB debugging, connect by USB, unlock and approve the computer.
Run CHECK_PHONE.cmd on Windows, or `sh CHECK_PHONE.sh` on Linux/macOS. These launchers only read facts; they do not install or delete anything.
The JSON is saved in phone-evidence. Add --install only if you want the collector to attempt an update without clearing data.
If Android rejects a different signing certificate, the collector leaves the existing app untouched.

APK SHA-256: {digest}
Source: {commit}
Review VIVO_TESTING.md for the airplane-mode and benchmark steps. This kit is not a device or performance result.
''', encoding='utf-8')
    windows = f'''@echo off
setlocal
where py >nul 2>nul
if errorlevel 1 (
  python "%~dp0check_phone.py" --label vivo-v21e --apk "%~dp0app-debug.apk" --expected-sha256 {digest} --out "%~dp0phone-evidence" %*
) else (
  py -3 "%~dp0check_phone.py" --label vivo-v21e --apk "%~dp0app-debug.apk" --expected-sha256 {digest} --out "%~dp0phone-evidence" %*
)
pause
'''
    (out / 'CHECK_PHONE.cmd').write_bytes(windows.replace('\n', '\r\n').encode('utf-8'))
    (out / 'CHECK_PHONE.sh').write_text(f'''#!/usr/bin/env sh
set -eu
kit_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec python3 "$kit_dir/check_phone.py" --label vivo-v21e --apk "$kit_dir/app-debug.apk" --expected-sha256 {digest} --out "$kit_dir/phone-evidence" "$@"
''', encoding='utf-8')
    (out / 'CHECK_PHONE.sh').chmod(0o755)
    return digest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    parser.add_argument('--commit', default='local-build')
    args = parser.parse_args()
    print('Kit created; APK SHA-256:', build(args.apk, args.out, args.commit))
