#!/usr/bin/env python3
"""Validate complete UI resources and format placeholders in every supported locale (B19)."""
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
        missing = base.keys() - translated.keys()
        assert not missing, f'{locale}: missing UI strings: {", ".join(sorted(missing))}'
        for key, text in translated.items():
            assert text.strip(), f'{locale}: empty translation: {key}'
            assert placeholders(text) == placeholders(base[key]), f'{locale}: format mismatch: {key}'
        print(f'{locale}: {len(translated)}/{len(base)} strings; all placeholders match')


if __name__ == '__main__':
    main()
