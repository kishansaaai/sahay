"""LLM layer for Sahay (PRD section 10): prompts, schemas, validate-retry-fallback orchestrator and task helpers.

Reference implementation in pure Python so the prompts, validators and evaluation run on a laptop and in CI.
The app calls the same prompt files from content/prompts/ through the on-device runtime.
"""
