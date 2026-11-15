#!/usr/bin/env python3
"""Read real Android device facts for B14/B15; optionally install a chosen debug APK without erasing data.

python scripts/check_phone.py --label vivo-v21e --apk app-debug.apk --out phone-evidence
python scripts/check_phone.py --label vivo-v21e --apk app-debug.apk --install --out phone-evidence

Requires Python 3 and Android platform-tools (adb). No third-party Python packages are needed.
Device identifiers/serials, learner data and account details are not written to the evidence file.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

PROPERTIES = {
    'manufacturer': 'ro.product.manufacturer', 'model': 'ro.product.model',
    'device': 'ro.product.device', 'android_release': 'ro.build.version.release',
    'sdk': 'ro.build.version.sdk', 'security_patch': 'ro.build.version.security_patch',
    'cpu_abis': 'ro.product.cpu.abilist', 'soc_model': 'ro.soc.model',
    'board_platform': 'ro.board.platform', 'vivo_os_version': 'ro.vivo.os.version',
}


def meminfo(text):
    fields = dict(re.findall(r'^(MemTotal|MemAvailable):\s+(\d+)\s+kB\s*$', text, re.M))
    return {name: int(fields[name]) if name in fields else None for name in ('MemTotal', 'MemAvailable')}


def positive_int(value):
    try:
        number = int(value)
        return number if number > 0 else None
    except (ValueError, TypeError):
        return None


def profile(properties, memory, airplane):
    sdk = positive_int(properties.get('sdk'))
    return {
        **{key: value or None for key, value in properties.items()},
        'sdk': sdk,
        'memory': {'kernel_managed_total_kib': memory['MemTotal'], 'available_kib': memory['MemAvailable']},
        'airplane_mode': {'0': False, '1': True}.get(airplane.strip()),
        'on_device_stt_api_supported': None if sdk is None else sdk >= 31,
        'on_device_stt_note': 'API support alone does not prove an offline recognizer or language pack is installed.',
        'memory_note': 'Kernel-managed RAM is not the manufacturer RAM rating; virtual/extended RAM is not counted.',
    }


class Adb:
    def __init__(self, executable, serial=None):
        self.executable, self.serial = executable, serial

    def call(self, *args, required=True, timeout=30):
        command = [self.executable] + (['-s', self.serial] if self.serial else []) + list(args)
        result = subprocess.run(command, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=timeout)
        if result.returncode and required:
            raise RuntimeError('ADB command failed: ' + (result.stderr.strip() or result.stdout.strip() or 'unknown error'))
        return result

    def select(self):
        rows = [line.split()[:2] for line in self.call('devices').stdout.splitlines()[1:] if line.strip()]
        if self.serial:
            state = next((state for serial, state in rows if serial == self.serial), None)
            if state != 'device':
                raise RuntimeError('Selected phone is not ready. Unlock it and approve USB debugging, then check adb devices.')
        else:
            devices = [serial for serial, state in rows if state == 'device']
            if len(devices) != 1:
                raise RuntimeError('Connect exactly one authorized phone, or select it with --serial. See adb devices.')
            self.serial = devices[0]


def find_adb(explicit):
    if explicit:
        return explicit
    executable = shutil.which('adb')
    if executable:
        return executable
    for variable in ('ANDROID_HOME', 'ANDROID_SDK_ROOT'):
        if os.environ.get(variable):
            path = Path(os.environ[variable]) / 'platform-tools' / ('adb.exe' if os.name == 'nt' else 'adb')
            if path.is_file():
                return str(path)
    raise RuntimeError('adb is missing. Install Android platform-tools and add it to PATH, or use --adb PATH.')


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1 << 20), b''):
            digest.update(block)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--adb', help='adb executable path, including adb.exe on Windows')
    parser.add_argument('--serial', help='select a device; never included in the report')
    parser.add_argument('--label', default='vivo-v21e', help='tester-assigned device label (not evidence of hardware identity)')
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--expected-sha256', help='compare the APK with its CI checksum before any installation')
    parser.add_argument('--install', action='store_true', help='run adb install -r; never uninstall or delete app data')
    parser.add_argument('--out', type=Path, default=Path('phone-evidence'))
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,60}', args.label):
        parser.error('--label must contain 1–60 letters, digits, underscores or hyphens')
    if args.install and not args.apk:
        parser.error('--install requires --apk')
    if args.expected_sha256 and not args.apk:
        parser.error('--expected-sha256 requires --apk')
    if args.expected_sha256 and not re.fullmatch(r'[0-9a-fA-F]{64}', args.expected_sha256):
        parser.error('--expected-sha256 must contain 64 hexadecimal digits')
    try:
        checksum = sha256(args.apk) if args.apk else None
        if args.expected_sha256 and checksum != args.expected_sha256.lower():
            raise RuntimeError('APK checksum mismatch; no installation was attempted.')
        adb = Adb(find_adb(args.adb), args.serial)
        adb.select()
        print('Reading the connected phone; no model, RAM or installed Android version is inferred from its marketing name.')
        properties = {name: adb.call('shell', 'getprop', prop).stdout.strip() for name, prop in PROPERTIES.items()}
        memory = meminfo(adb.call('shell', 'cat', '/proc/meminfo').stdout)
        airplane = adb.call('shell', 'settings', 'get', 'global', 'airplane_mode_on', required=False).stdout
        result = profile(properties, memory, airplane)
        result.update(schema='sahay.phone-evidence/1', label=args.label, captured_at=dt.datetime.now(dt.timezone.utc).isoformat(),
                      apk_sha256=checksum, installation_attempted=args.install)
        if args.install:
            installed = adb.call('install', '-r', str(args.apk.resolve()), required=False, timeout=120)
            result['installation_succeeded'] = installed.returncode == 0 and 'Success' in installed.stdout
            if not result['installation_succeeded']:
                result['installation_error'] = installed.stderr.strip() or installed.stdout.strip()
                print('Installation did not succeed. No uninstall or data deletion was attempted.')
        result['app_package_present'] = adb.call('shell', 'pm', 'path', 'com.sahay.app', required=False).stdout.strip().startswith('package:')
        args.out.mkdir(parents=True, exist_ok=True)
        stamp = dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        path = args.out / (args.label + '-' + stamp + '.json')
        with path.open('x', encoding='utf-8') as destination:
            json.dump(result, destination, ensure_ascii=False, indent=2)
            destination.write('\n')
        print('Model:', result['model'], '| Android:', result['android_release'], '| SDK:', result['sdk'])
        print('Kernel-managed RAM (KiB):', result['memory']['kernel_managed_total_kib'])
        print('Wrote measured facts to', path)
        if result['on_device_stt_api_supported'] is False:
            print('This installed Android version lacks the Android 12 on-device STT API. Test typing/TTS; keep voice marked Prototype.')
        print('Run the airplane-mode and benchmark steps in docs/VIVO_TESTING.md; this file is not a performance result.')
        return 1 if args.install and not result.get('installation_succeeded') else 0
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
