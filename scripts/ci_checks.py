#!/usr/bin/env python3
"""CI checks: no INTERNET permission in any manifest; content JSON files parse."""
import argparse, json, sys
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent.parent
errors = []
parser = argparse.ArgumentParser()
parser.add_argument("--merged-manifest", action="append", type=Path, default=[])
args = parser.parse_args()
android = "{http://schemas.android.com/apk/res/android}"
tools = "{http://schemas.android.com/tools}"
for m in [p for p in root.rglob("AndroidManifest.xml") if "build" not in p.parts] + args.merged_manifest:
    try:
        manifest = ET.parse(m).getroot()
        for permission in manifest:
            if permission.tag.startswith("uses-permission") and permission.get(android + "name") == "android.permission.INTERNET":
                if permission.get(tools + "node") != "remove":
                    errors.append("INTERNET permission found in %s" % m)
    except (OSError, ET.ParseError) as e:
        errors.append("Cannot validate manifest %s: %s" % (m, e))
for j in (root / "content").rglob("*.json"):
    try:
        json.loads(j.read_text(encoding="utf-8"))
    except Exception as e:
        errors.append("Invalid JSON %s: %s" % (j.relative_to(root), e))
if errors:
    print("\n".join(errors)); sys.exit(1)
print("ci_checks OK")
