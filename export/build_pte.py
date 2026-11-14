#!/usr/bin/env python3
"""Reproducible export of Llama 3.2 1B Instruct to an INT4 ExecuTorch .pte (issue A03, PRD 10.1).

    python export/build_pte.py --checkpoint-dir ~/models/Llama3.2-1B-Instruct --out build/llama3_2_1b_int4.pte
    python export/build_pte.py --checkpoint-dir ... --variant int8 --dry-run      # print the command only

Prerequisites (see export/README.md): Llama licence accepted (A02), ExecuTorch checked out at the tag in export/EXECUTORCH_VERSION
with its Python environment installed, and the checkpoint directory holding consolidated.00.pth and params.json.

The script (1) verifies inputs, (2) runs the ExecuTorch Llama export with the recipe below, (3) records the SHA-256, size and exact
command in releases/CHECKSUMS.txt and build/<name>.recipe.json. The .pte is written under build/ which is git-ignored; it is never committed.
"""
import argparse
import datetime
import hashlib
import json
import os
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CTX = 2048  # PRD 10.1 context cap
METADATA = '{"get_bos_id":128000,"get_eos_ids":[128009,128001]}'  # Llama 3.x special tokens

# Hydra reads this as a quoted string, as in the ExecuTorch Llama guide (the shell quoting there is not needed: we pass argv directly).
HYDRA_METADATA = '"' + METADATA.replace('"', '\\"') + '"'

# Recipe names follow the ExecuTorch Llama guide. They change between releases: confirm against the docs for the pinned tag.
VARIANTS = {
    "int4": {"qmode": "8da4w", "group": "32", "embedding": "4,32", "desc": "4-bit weights (group 32), 8-bit dynamic activations, 4-bit embeddings"},
    "int8": {"qmode": "8da8w", "group": None, "embedding": "8,0", "desc": "8-bit weights, 8-bit dynamic activations (INT8 comparison)"},
}
MODELS = {"1b": "llama3_2", "3b": "llama3_2"}


def sha256(path, block=1 << 20):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(block):
            h.update(chunk)
    return h.hexdigest()


def command(args, ckpt, params, out):
    v = VARIANTS[args.variant]
    if args.cli_style == "new":  # ExecuTorch >= 0.7 hydra-style entry point
        c = ["python", "-m", "extension.llm.export.export_llm",
             "base.model_class=%s" % MODELS[args.size], "base.checkpoint=%s" % ckpt, "base.params=%s" % params,
             "base.metadata=%s" % HYDRA_METADATA, "model.use_kv_cache=True", "model.use_sdpa_with_kv_cache=True", "model.dtype_override=fp32",
             "backend.xnnpack.enabled=True", "backend.xnnpack.extended_ops=True", "quantization.qmode=%s" % v["qmode"],
             "quantization.embedding_quantize='%s'" % v["embedding"],
             "export.max_seq_length=%d" % CTX, "export.max_context_length=%d" % CTX, "export.output_name=%s" % out]
        if v["group"]:
            c.append("quantization.group_size=%s" % v["group"])
    else:  # earlier releases
        c = ["python", "-m", "examples.models.llama.export_llama", "--checkpoint", str(ckpt), "-p", str(params), "-kv", "--use_sdpa_with_kv_cache",
             "-X", "--xnnpack-extended-ops", "-qmode", v["qmode"], "-d", "fp32", "--embedding-quantize", v["embedding"],
             "--max_seq_length", str(CTX), "--max_context_length", str(CTX), "--metadata", METADATA, "--output_name", str(out)]
        if v["group"]:
            c += ["--group_size", v["group"]]
    return c


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--checkpoint-dir", required=True, type=Path)
    ap.add_argument("--executorch-dir", type=Path, default=Path(os.environ.get("EXECUTORCH_DIR", "executorch")))
    ap.add_argument("--variant", choices=sorted(VARIANTS), default="int4")
    ap.add_argument("--size", choices=sorted(MODELS), default="1b")
    ap.add_argument("--cli-style", choices=["new", "legacy"], default="new")
    ap.add_argument("--out", type=Path)
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    name = "llama3_2_%s_%s" % (a.size, a.variant)
    out = (a.out or ROOT / "build" / (name + ".pte")).resolve()
    ckpt, params = a.checkpoint_dir / "consolidated.00.pth", a.checkpoint_dir / "params.json"
    cmd = command(a, ckpt.resolve(), params.resolve(), out)
    print("recipe:", VARIANTS[a.variant]["desc"])
    print("command:", shlex.join(cmd))
    if a.dry_run:
        return 0
    for p, what in ((ckpt, "checkpoint"), (params, "params.json"), (a.executorch_dir / "examples", "ExecuTorch checkout (--executorch-dir)")):
        if not p.exists():
            sys.exit("missing %s: %s (accept the Llama licence and download the weights first, see export/README.md)" % (what, p))
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(cmd, cwd=a.executorch_dir, check=True)
    if not out.exists():
        sys.exit("export finished but %s was not created" % out)
    digest, size = sha256(out), out.stat().st_size
    ver = (ROOT / "export" / "EXECUTORCH_VERSION").read_text().strip() if (ROOT / "export" / "EXECUTORCH_VERSION").exists() else "unpinned"
    rec = {"name": out.name, "sha256": digest, "bytes": size, "variant": a.variant, "size": a.size, "executorch": ver, "command": shlex.join(cmd),
           "date": datetime.date.today().isoformat()}
    out.with_suffix(".recipe.json").write_text(json.dumps(rec, indent=1) + "\n")
    line = "%s  %s  %d  %s  executorch=%s  %s\n" % (digest, out.name, size, a.variant, ver, rec["date"])
    ck = ROOT / "releases" / "CHECKSUMS.txt"
    lines = [l for l in ck.read_text().splitlines(True) if l.startswith("#") or l.strip() and l.split()[1] != out.name]
    ck.write_text("".join(lines) + line)
    print("wrote %s (%.0f MB) sha256=%s\nrecorded in releases/CHECKSUMS.txt. Do NOT commit the .pte." % (out, size / 1e6, digest))
    return 0


if __name__ == "__main__":
    sys.exit(main())
