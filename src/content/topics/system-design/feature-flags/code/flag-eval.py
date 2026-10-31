import hashlib
from dataclasses import dataclass, field

BUCKETS = 100_000


@dataclass
class Rule:
    attribute: str
    values: set[str]
    variation: str


@dataclass
class Flag:
    key: str
    on: bool
    salt: str
    off_variation: str
    targets: dict[str, str] = field(default_factory=dict)  # user id -> variation
    rules: list[Rule] = field(default_factory=list)
    rollout: list[tuple[str, int]] = field(default_factory=list)  # (variation, weight of 100,000)


def bucket(flag_key: str, salt: str, user_id: str) -> int:
    """The same user always gets the same bucket for a flag, in every SDK language."""
    h = hashlib.sha256(f"{flag_key}:{salt}:{user_id}".encode()).digest()
    return int.from_bytes(h[:4], "big") % BUCKETS


def evaluate(flag: Flag, user_id: str, attrs: dict[str, str]) -> tuple[str, str]:
    if not flag.on:
        return flag.off_variation, "OFF"
    if user_id in flag.targets:
        return flag.targets[user_id], "TARGET"
    for i, r in enumerate(flag.rules):
        if attrs.get(r.attribute) in r.values:
            return r.variation, f"RULE {i}"
    # Walk the slices: bucket 0..99,999 falls into exactly one of them.
    b, up_to = bucket(flag.key, flag.salt, user_id), 0
    for variation, weight in flag.rollout:
        up_to += weight
        if b < up_to:
            return variation, "ROLLOUT"
    return flag.off_variation, "ROLLOUT"
