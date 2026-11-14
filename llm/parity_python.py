#!/usr/bin/env python3
"""Prints the SHA-256 of the prompt built for every task, language and example input, so CI can diff it against the Kotlin
port (engine/src/test/kotlin/.../LlmParity.kt). Run from the repository root."""
import hashlib, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from llm.sahay_llm import prompts

for t in prompts.TASKS:
    spec = prompts.load_task(t)
    for i, e in enumerate(spec["examples"]):
        text = prompts.to_llama3(prompts.build_messages(t, e["input"], e["lang"]))
        retry = prompts.to_llama3(prompts.build_messages(t, e["input"], e["lang"], "some error", "bad reply"))
        print(t, i, hashlib.sha256(text.encode("utf-8")).hexdigest()[:16], hashlib.sha256(retry.encode("utf-8")).hexdigest()[:16])
