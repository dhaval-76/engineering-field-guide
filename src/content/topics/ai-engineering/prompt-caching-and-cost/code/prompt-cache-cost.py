import math
from dataclasses import dataclass


@dataclass(frozen=True)
class Pricing:
    """Prices are per million tokens. Multipliers apply to the input price."""

    input: float
    output: float
    write_multiplier: float
    read_multiplier: float


@dataclass(frozen=True)
class Workload:
    requests: int
    prefix: int
    suffix: int
    output: int
    hit_rate: float


def cost_without_cache(w, p):
    input_tokens = w.requests * (w.prefix + w.suffix)
    return (input_tokens * p.input + w.requests * w.output * p.output) / 1e6


def cost_with_cache(w, p):
    hits = w.requests * w.hit_rate
    misses = w.requests - hits
    prefix_units = misses * w.prefix * p.write_multiplier + hits * w.prefix * p.read_multiplier
    input_units = prefix_units + w.requests * w.suffix
    return (input_units * p.input + w.requests * w.output * p.output) / 1e6


def reads_to_break_even(p):
    """How many cache reads repay one write."""
    if p.write_multiplier <= 1:
        return 0
    return math.ceil((p.write_multiplier - 1) / (1 - p.read_multiplier))
