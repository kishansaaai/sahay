#!/usr/bin/env python3
"""Verify a downloaded model file against releases/CHECKSUMS.txt (issue A19).   python scripts/verify_model.py path/to/model.pte"""
import hashlib, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def load():
    out = {}
    for l in (ROOT / "releases" / "CHECKSUMS.txt").read_text().splitlines():
        if l.strip() and not l.startswith("#"):
            p = l.split()
            out[p[1]] = (p[0], int(p[2]))
    return out


def main(path):
    p, table = Path(path), load()
    if p.name not in table:
        sys.exit("%s is not listed in releases/CHECKSUMS.txt" % p.name)
    h = hashlib.sha256()
    with open(p, "rb") as f:
        while c := f.read(1 << 20):
            h.update(c)
    want, size = table[p.name]
    if h.hexdigest() != want or p.stat().st_size != size:
        sys.exit("MISMATCH for %s: got %s, expected %s" % (p.name, h.hexdigest(), want))
    print("OK", p.name, want)


if __name__ == "__main__":
    main(sys.argv[1]) if len(sys.argv) == 2 else sys.exit(__doc__)
