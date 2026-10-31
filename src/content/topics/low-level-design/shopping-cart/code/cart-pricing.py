from dataclasses import dataclass
from typing import Callable

@dataclass(frozen=True)
class Adjustment:
    label: str
    amount: int            # paise; negative = discount

@dataclass(frozen=True)
class Promotion:
    group: str             # ITEM → ORDER_PERCENT → COUPON: groups apply in order, best offer per group
    evaluate: Callable[[list, int], Adjustment | None]

def percent_off_above(threshold: int, pct: int) -> Promotion:
    return Promotion("ORDER_PERCENT",
                     lambda lines, total: Adjustment(f"{pct}% off", -(total * pct // 100)) if total >= threshold else None)

def flat_coupon(code: str, off: int, minimum: int = 0) -> Promotion:
    return Promotion("COUPON",
                     lambda lines, total: Adjustment(f"Coupon {code}", -min(off, total)) if total >= minimum else None)

def price(lines: list[dict], promotions: list[Promotion], tax_rate: float) -> dict:
    """lines: [{"sku", "qty", "unit_paise"}]. Integer paise throughout; tax rounded once at the end."""
    total = sum(l["unit_paise"] * l["qty"] for l in lines)
    applied = []
    for group in ("ITEM", "ORDER_PERCENT", "COUPON"):
        offers = [a for p in promotions if p.group == group if (a := p.evaluate(lines, total))]
        if offers:
            best = min(offers, key=lambda a: a.amount)     # most negative = biggest discount
            applied.append(best)
            total += best.amount
    tax = round(total * tax_rate)
    return {"applied": applied, "tax": tax, "payable": total + tax}

# price(lines, [percent_off_above(1_000_000, 10), flat_coupon("SAVE500", 50_000)], 0.18)
