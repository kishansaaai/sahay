#!/usr/bin/env python3
"""CI checks: no INTERNET permission in any manifest; content JSON files parse."""
import json, sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
errors = []
for m in root.rglob("AndroidManifest.xml"):
    if "build" in m.parts:
        continue
    if "android.permission.INTERNET" in m.read_text(encoding="utf-8", errors="ignore"):
        errors.append("INTERNET permission found in %s" % m.relative_to(root))
for j in (root / "content").rglob("*.json"):
    try:
        json.loads(j.read_text(encoding="utf-8"))
    except Exception as e:
        errors.append("Invalid JSON %s: %s" % (j.relative_to(root), e))
if errors:
    print("\n".join(errors)); sys.exit(1)
print("ci_checks OK")
