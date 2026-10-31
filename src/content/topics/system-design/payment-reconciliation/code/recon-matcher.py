from dataclasses import dataclass

DATE_WINDOW = 2  # days a settlement may lag or lead
TRANSIT_DAYS = 3  # how long an unmatched payment may wait


# Amounts in minor units (paise, cents); dates as day numbers.
@dataclass(frozen=True)
class Payment:
    id: str
    ref: str
    amount: int
    currency: str
    day: int


@dataclass(frozen=True)
class SettlementLine:
    line_id: str
    ref: str
    amount: int
    currency: str
    day: int


def reconcile(ours: list[Payment], theirs: list[SettlementLine], today: int):
    out = []
    by_ref = {p.ref: p for p in ours}
    matched: set[str] = set()
    leftover = []

    # Pass 1: the provider echoed our reference, so match on it exactly.
    for line in theirs:
        p = by_ref.get(line.ref)
        if p is None:
            leftover.append(line)
        elif p.id in matched:
            out.append(("DUPLICATE", p.id, line.line_id))
        else:
            matched.add(p.id)
            same = p.amount == line.amount and p.currency == line.currency
            out.append(("MATCHED" if same else "AMOUNT_MISMATCH", p.id, line.line_id))

    # Pass 2: no usable reference; pair by amount and currency, closest date first.
    open_by_amount: dict[tuple[int, str], list[Payment]] = {}
    for p in ours:
        if p.id not in matched:
            open_by_amount.setdefault((p.amount, p.currency), []).append(p)
    for line in leftover:
        candidates = open_by_amount.get((line.amount, line.currency), [])
        in_window = [p for p in candidates if abs(p.day - line.day) <= DATE_WINDOW]
        if not in_window:
            out.append(("MISSING_IN_LEDGER", None, line.line_id))
            continue
        best = min(in_window, key=lambda p: abs(p.day - line.day))
        candidates.remove(best)
        matched.add(best.id)
        out.append(("MATCHED_BY_AMOUNT", best.id, line.line_id))

    # Whatever is left on our side is either still in transit or a real break.
    for p in ours:
        if p.id not in matched:
            status = "IN_TRANSIT" if today - p.day <= TRANSIT_DAYS else "MISSING_AT_PROVIDER"
            out.append((status, p.id, None))
    return out
