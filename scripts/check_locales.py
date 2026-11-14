#!/usr/bin/env python3
"""Validate complete English/Hindi resources and intentional partial additional locale packs (B19)."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res'


def strings(folder):
    rows = ET.parse(ROOT / folder / 'strings.xml').getroot().findall('string')
    result = {row.attrib['name']: ''.join(row.itertext()) for row in rows}
    assert len(result) == len(rows), f'{folder}: duplicate string name'
    return result


def placeholders(value):
    return sorted(re.findall(r'%(?:\d+\$)?[dsf]', value.replace('%%', '')))


def main():
    base = strings('values')
    for locale in ('hi', 'ta', 'te', 'bn', 'mr'):
        translated = strings('values-' + locale)
        assert translated.keys() <= base.keys(), f'{locale}: unknown resource key'
        if locale == 'hi':
            assert translated.keys() == base.keys(), 'Hindi must cover every UI string'
        else:
            assert {'nav_today', 'nav_roadmap', 'nav_tutor', 'nav_dashboard', 'settings_title', 'scan_title', 'share_title'} <= translated.keys(), f'{locale}: missing core navigation'
        for key, text in translated.items():
            assert text.strip(), f'{locale}: empty translation: {key}'
            assert placeholders(text) == placeholders(base[key]), f'{locale}: format mismatch: {key}'
        print(f'{locale}: {len(translated)}/{len(base)} strings; {len(base) - len(translated)} intentional English fallbacks')


if __name__ == '__main__':
    main()
