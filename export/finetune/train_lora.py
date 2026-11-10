#!/usr/bin/env python3
"""LoRA fine-tune of Llama 3.2 1B Instruct for tutoring tone and strict JSON (issue A18, PRD 10.7). Run on Colab with a GPU.

    pip install transformers peft datasets accelerate
    python train_lora.py --data sft.jsonl --base meta-llama/Llama-3.2-1B-Instruct --out lora-out

NOT RUN YET: written without GPU access, so expect to adjust versions. After training: merge, export and evaluate as in README.md.
Training examples are rendered with exactly the prompt the app uses (content/prompts), so the tuned model sees the production format.
"""
import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from llm.sahay_llm import prompts  # noqa: E402


def render(r):
    msgs = prompts.build_messages(r["task"], r["input"], r["language"])
    return prompts.to_llama3(msgs) + json.dumps(r["output"], ensure_ascii=False, separators=(",", ":")) + "<|eot_id|>", len(prompts.to_llama3(msgs))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True); ap.add_argument("--base", default="meta-llama/Llama-3.2-1B-Instruct"); ap.add_argument("--out", default="lora-out")
    ap.add_argument("--epochs", type=int, default=2); ap.add_argument("--rank", type=int, default=16); ap.add_argument("--lr", type=float, default=2e-4)
    a = ap.parse_args()
    import torch
    from datasets import Dataset
    from peft import LoraConfig, get_peft_model
    from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments

    tok = AutoTokenizer.from_pretrained(a.base)
    rows = [json.loads(l) for l in open(a.data, encoding="utf-8") if l.strip()]

    def enc(r):
        text, cut = render(r)
        ids = tok(text, add_special_tokens=False, truncation=True, max_length=2048)["input_ids"]
        n_prompt = len(tok(text[:cut], add_special_tokens=False)["input_ids"])
        return {"input_ids": ids, "labels": [-100] * n_prompt + ids[n_prompt:]}  # loss on the answer only

    ds = Dataset.from_list([enc(r) for r in rows])
    model = AutoModelForCausalLM.from_pretrained(a.base, torch_dtype=torch.bfloat16)
    model = get_peft_model(model, LoraConfig(r=a.rank, lora_alpha=2 * a.rank, lora_dropout=0.05, target_modules=["q_proj", "k_proj", "v_proj", "o_proj"], task_type="CAUSAL_LM"))

    def collate(batch):
        m = max(len(b["input_ids"]) for b in batch)
        pad = lambda x, v: x + [v] * (m - len(x))
        return {"input_ids": torch.tensor([pad(b["input_ids"], tok.pad_token_id or 0) for b in batch]),
                "labels": torch.tensor([pad(b["labels"], -100) for b in batch]),
                "attention_mask": torch.tensor([pad([1] * len(b["input_ids"]), 0) for b in batch])}

    Trainer(model=model, train_dataset=ds, data_collator=collate,
            args=TrainingArguments(a.out, num_train_epochs=a.epochs, per_device_train_batch_size=4, gradient_accumulation_steps=4, learning_rate=a.lr,
                                   bf16=True, logging_steps=10, save_strategy="epoch", report_to=[], remove_unused_columns=False)).train()
    model.save_pretrained(a.out)
    print("adapter saved to", a.out)


if __name__ == "__main__":
    main()
