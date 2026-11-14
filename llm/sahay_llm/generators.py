"""Pluggable text generators for the orchestrator and the evaluation harness."""
import subprocess
from . import prompts


class CommandGenerator:
    """Runs a shell command with the Llama-3-formatted prompt on stdin and returns stdout.
    Use it to plug in the exported model on a laptop (for example a script around the ExecuTorch runner)
    or on the phone (`adb shell ...`). The command receives SAHAY_TEMPERATURE and SAHAY_MAX_TOKENS in its environment."""

    def __init__(self, command, timeout=300):
        self.command, self.timeout = command, timeout

    def __call__(self, messages, temperature, max_tokens):
        import os
        env = dict(os.environ, SAHAY_TEMPERATURE=str(temperature), SAHAY_MAX_TOKENS=str(max_tokens))
        p = subprocess.run(self.command, shell=True, input=prompts.to_llama3(messages), capture_output=True,
                           text=True, timeout=self.timeout, env=env)
        if p.returncode != 0:
            raise RuntimeError("generator exited %d: %s" % (p.returncode, p.stderr.strip()[:200]))
        return p.stdout


class ScriptedGenerator:
    """Returns the queued replies in order (for tests)."""

    def __init__(self, replies):
        self.replies, self.calls = list(replies), []

    def __call__(self, messages, temperature, max_tokens):
        self.calls.append((messages, temperature, max_tokens))
        return self.replies.pop(0) if self.replies else ""
