from decimal import Decimal, ROUND_HALF_UP
from typing import Callable

# Amounts are integers in the smallest currency unit (paise). Each strategy returns shares
# summing exactly to total.
SplitStrategy = Callable[[int, list[str]], dict[str, int]]

def equal_split(total: int, people: list[str]) -> dict[str, int]:
    base, remainder = divmod(total, len(people))
    # leftover paise, deterministically
    return {p: base + (1 if i < remainder else 0) for i, p in enumerate(people)}

def exact_split(amounts: dict[str, int]) -> SplitStrategy:
    def split(total: int, people: list[str]) -> dict[str, int]:
        if sum(amounts.values()) != total:
            raise ValueError(f"exact amounts add up to {sum(amounts.values())}, not {total}")
        return dict(amounts)
    return split

def percent_split(percent: dict[str, Decimal]) -> SplitStrategy:
    if sum(percent.values()) != Decimal(100):
        raise ValueError("percentages must add up to 100")
    def split(total: int, people: list[str]) -> dict[str, int]:
        out, assigned = {}, 0
        for i, p in enumerate(people):
            if i == len(people) - 1:
                # last person absorbs rounding
                share = total - assigned
            else:
                share = int((percent[p] * total / 100).quantize(Decimal(1), rounding=ROUND_HALF_UP))
            out[p] = share
            assigned += share
        return out
    return split
