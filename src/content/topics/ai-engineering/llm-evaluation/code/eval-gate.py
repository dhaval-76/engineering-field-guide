import math
from dataclasses import dataclass
from typing import Callable


@dataclass(frozen=True)
class EvalCase:
    id: str
    input: str
    passes: Callable[[str], bool]


@dataclass(frozen=True)
class EvalRun:
    total: int
    failed: frozenset


def run(cases, model):
    """Runs every case once and records which ones fail."""
    failed = frozenset(c.id for c in cases if not c.passes(model(c.input)))
    return EvalRun(len(cases), failed)


def wilson(passed, total, z=1.96):
    """95% Wilson interval for a pass rate: how far the true rate may be from what you measured."""
    p = passed / total
    centre = (p + z * z / (2 * total)) / (1 + z * z / total)
    margin = z * math.sqrt(p * (1 - p) / total + z * z / (4 * total * total)) / (1 + z * z / total)
    return centre - margin, centre + margin


def sign_test(worse, better):
    """Two-sided sign test on the cases that changed; unchanged cases carry no signal."""
    n = worse + better
    if n == 0:
        return 1.0
    tail = sum(math.comb(n, i) for i in range(min(worse, better) + 1)) * 0.5**n
    return min(1.0, 2 * tail)


def compare(baseline, candidate):
    """Compares the same cases under two versions. Matching cases makes small changes detectable."""
    new_failures = sorted(candidate.failed - baseline.failed)
    fixed = sorted(baseline.failed - candidate.failed)
    return new_failures, fixed, sign_test(len(new_failures), len(fixed))
