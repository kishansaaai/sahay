#!/usr/bin/env python3
"""Checks the Android string resources (B19): every locale parses, uses only keys that exist in the default file, keeps
the same format placeholders as English, and contains nothing aapt rejects (an unescaped apostrophe).
Hindi must be complete. Other locales may be partial (missing keys fall back to English) but are reported."""
import re, sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

res = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]|%%")
COMPLETE = {"hi"}
errors = []


def load(path):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as e:
        errors.append("%s does not parse: %s" % (path.relative_to(res), e))
        return {}
    return {e.get("name"): "".join(e.itertext()) for e in root.iter("string")}


def check_text(where, name, text):
    if re.search(r"(?<!\\)'", text) and not (text.startswith('"') and text.endswith('"')):
        errors.append("%s: %s has an unescaped apostrophe (write \\')" % (where, name))


default = load(res / "values" / "strings.xml")
for name, text in default.items():
    check_text("values", name, text)

report = []
for d in sorted(res.glob("values-*")):
    f = d / "strings.xml"
    if not f.exists():
        continue
    code = d.name.split("-", 1)[1]
    strings = load(f)
    for name, text in strings.items():
        if name not in default:
            errors.append("%s: %s is not in the default strings" % (d.name, name))
            continue
        check_text(d.name, name, text)
        if Counter(PLACEHOLDER.findall(text)) != Counter(PLACEHOLDER.findall(default[name])):
            errors.append("%s: %s placeholders differ from English (%s vs %s)" % (
                d.name, name, PLACEHOLDER.findall(text), PLACEHOLDER.findall(default[name])))
    missing = sorted(set(default) - set(strings))
    if code in COMPLETE and missing:
        errors.append("%s must be complete, missing: %s" % (d.name, ", ".join(missing[:10])))
    report.append("%s: %d of %d strings%s" % (d.name, len(strings), len(default), " (complete)" if not missing else ""))

if errors:
    print("\n".join(errors)); sys.exit(1)
print("check_strings OK\n" + "\n".join(report))
