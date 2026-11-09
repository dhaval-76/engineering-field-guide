import hashlib
import re


def first_user_text(example):
    return next((t["content"] for t in example["turns"] if t["role"] == "user"), "")


def normalize(text):
    return re.sub(r"\s+", " ", text.strip().lower())


def validate(example):
    """Problems that would waste a training run. An empty list means the example is usable."""
    turns = example["turns"]
    if not turns:
        return ["no turns"]
    problems = []
    if not first_user_text(example).strip():
        problems.append("no user message")
    if turns[-1]["role"] != "assistant" or not turns[-1]["content"].strip():
        problems.append("must end with a non-empty assistant message")
    return problems


def split(examples, validation_percent):
    """Splits by a hash of the question, so the same example always lands on the same side.

    A random split lets near-identical questions leak into validation and inflate the score.
    """
    unique = {}
    for example in examples:
        unique.setdefault(normalize(first_user_text(example)), example)
    train, validation = [], []
    for key, example in unique.items():
        bucket = hashlib.sha256(key.encode()).digest()[0] % 100
        (validation if bucket < validation_percent else train).append(example)
    return train, validation
