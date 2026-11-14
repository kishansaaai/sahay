#!/usr/bin/env python3
"""CI checks: no INTERNET permission in any manifest; content JSON files parse.

A source manifest may name the permission only to remove it (tools:node="remove"), which is how the app strips what
library manifests such as ML Kit merge in. Pass a merged manifest path to check the final result, which must have none:
    python scripts/ci_checks.py --merged app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml
"""
import json, sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID = "{http://schemas.android.com/apk/res/android}"
TOOLS = "{http://schemas.android.com/tools}"
root = Path(__file__).resolve().parent.parent
errors = []


def internet_grants(path, allow_remove):
    """Count INTERNET permission declarations that would grant it."""
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        errors.append("Manifest %s does not parse: %s" % (path, e))
        return 0
    n = 0
    for el in tree.getroot().iter():
        if el.tag not in ("uses-permission", "uses-permission-sdk-23"):
            continue
        if el.get(ANDROID + "name") == "android.permission.INTERNET":
            if allow_remove and el.get(TOOLS + "node") == "remove":
                continue
            n += 1
    return n


merged = [Path(a) for a in sys.argv[2:]] if len(sys.argv) > 1 and sys.argv[1] == "--merged" else []
for m in merged:
    if not m.exists():
        errors.append("Merged manifest not found: %s" % m)
    elif internet_grants(m, allow_remove=False):
        errors.append("INTERNET permission found in merged manifest %s" % m)
if not merged:
    for m in root.rglob("AndroidManifest.xml"):
        if "build" in m.parts:
            continue
        if internet_grants(m, allow_remove=True):
            errors.append("INTERNET permission found in %s" % m.relative_to(root))
    for j in (root / "content").rglob("*.json"):
        try:
            json.loads(j.read_text(encoding="utf-8"))
        except Exception as e:
            errors.append("Invalid JSON %s: %s" % (j.relative_to(root), e))
if errors:
    print("\n".join(errors)); sys.exit(1)
print("ci_checks OK")
