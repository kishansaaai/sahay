# Prompt prefix / KV-cache reuse experiment (issue A17, feature X4)

Every prompt starts with the same system prompt (`content/prompts/system.txt`, about 130 tokens by our estimate, limit 300), so in principle the
prefix's KV cache could be computed once and reused. Whether the ExecuTorch Android runner supports it is unknown to us.

## Step 1: ask the mentors (still to send; the email below is drafted, not sent)

Email tech-queries@armbharatchallenge.com, subject starting `PS4`:

> Subject: PS4 - prompt prefix / KV-cache reuse in the ExecuTorch Android Llama runner
>
> We run Llama 3.2 1B Instruct INT4 via ExecuTorch (tag: <TAG>) with a fixed ~130-token system prompt on every request. Does the Android
> LlmModule / runner support keeping the KV cache between generate() calls so the shared prefix is not re-processed (or a prefill-only
> call followed by generate), and which API or flag do you recommend? If not, is a custom runner loop acceptable for Phase 1?
> Team: <name>. Phone: <model>.

Record the date sent and the reply here: ____

## What the ExecuTorch Android API allows (checked 2026-11-14)

`LlmModule` in `org.pytorch.executorch.extension.llm` (the class the official demo and our `ExecuTorchRuntime` use) exposes only these cache-related calls:

| Call | Effect |
| --- | --- |
| `prefillPrompt(String)` | runs the prefill phase for a prompt into the KV cache |
| `generate(prompt, seqLen, callback)` | prefills that prompt itself, then decodes |
| `resetContext()` | clears the KV cache and sets the start position back to 0 |
| `stop()` | stops the current `generate()` |

No call trims the cache back to a shorter shared prefix: the only reset is a full `resetContext()`. A prompt can therefore reuse a prefilled prefix only when it is an exact extension of the prefilled text. Sahay's prompts share the ~130-token system prompt and then diverge (task template, skill, learner state), so they are not exact extensions and prefix reuse is not possible through this API. Doing it properly needs a custom runner loop over the C++ `IRunner` (prefill the shared head, then feed only the unmatched suffix while tracking positions), which is out of scope for Phase 1.

Sources: `docs.pytorch.org/executorch/stable/llm/run-on-android.html` (LlmModule: generate, stop, resetContext) and the `LlmModule` Javadoc (`prefillPrompt`, `resetContext`).

## Step 2: measure (if supported)

- Condition A: full prompt each time. Condition B: prefix cached. Same 10 prompts, 10 runs each, cool start, same conditions as A06.
- Report median and p95 time to first token for A and B and the saving in ms and percent.
- Cheaper variant that needs no runtime support: shorten the system prompt (for example to 60 tokens) and report the time-to-first-token saving of prompt processing at the measured prompt speed (A06). Keep the validity rate from `eval/run_eval.py` for both prompts, since a shorter prompt may hurt JSON validity.

## Step 3: result or reason

**Status: documented on 2026-11-14, experiment not run.** The public `LlmModule` API has no partial cache rollback (see above), so a
with/without comparison cannot be built without a custom runner. What is still worth measuring is the cheaper variant from Step 2: the
time-to-first-token saving from shortening the system prompt from about 130 tokens to about 60, scaled by the prompt-processing speed from
A06. That needs the phone, so it stays open together with A06. The mentor email in Step 1 still has to be sent and its reply recorded.
