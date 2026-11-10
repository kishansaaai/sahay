# Prompt prefix / KV-cache reuse experiment (issue A17, feature X4)

Every prompt starts with the same system prompt (`content/prompts/system.txt`, about 130 tokens by our estimate, limit 300), so in principle the
prefix's KV cache could be computed once and reused. Whether the ExecuTorch Android runner supports it is unknown to us.

## Step 1: ask the mentors (do this first)

Email tech-queries@armbharatchallenge.com, subject starting `PS4`:

> Subject: PS4 - prompt prefix / KV-cache reuse in the ExecuTorch Android Llama runner
>
> We run Llama 3.2 1B Instruct INT4 via ExecuTorch (tag: <TAG>) with a fixed ~130-token system prompt on every request. Does the Android
> LlmModule / runner support keeping the KV cache between generate() calls so the shared prefix is not re-processed (or a prefill-only
> call followed by generate), and which API or flag do you recommend? If not, is a custom runner loop acceptable for Phase 1?
> Team: <name>. Phone: <model>.

Record the date sent and the reply here: ____

## Step 2: measure (if supported)

- Condition A: full prompt each time. Condition B: prefix cached. Same 10 prompts, 10 runs each, cool start, same conditions as A06.
- Report median and p95 time to first token for A and B and the saving in ms and percent.
- Cheaper variant that needs no runtime support: shorten the system prompt (for example to 60 tokens) and report the time-to-first-token saving of prompt processing at the measured prompt speed (A06). Keep the validity rate from `eval/run_eval.py` for both prompts, since a shorter prompt may hurt JSON validity.

## Step 3: result or reason

If unsupported, document why (mentor reply, API missing) and keep the shorter-prompt number. Status: not started.
