import heapq


def simplify(net_balances: dict[str, int]) -> list[tuple[str, str, int]]:
    """net_balances in paise; positive = is owed money. Returns (from, to, amount) transfers."""
    creditors = [(-bal, p) for p, bal in net_balances.items() if bal > 0]  # max-heaps via negation
    debtors = [(bal, p) for p, bal in net_balances.items() if bal < 0]
    heapq.heapify(creditors)
    heapq.heapify(debtors)
    plan = []
    while creditors and debtors:
        credit, c = heapq.heappop(creditors)
        debt, d = heapq.heappop(debtors)
        amount = min(-credit, -debt)
        plan.append((d, c, amount))
        if -credit > amount:
            heapq.heappush(creditors, (credit + amount, c))
        if -debt > amount:
            heapq.heappush(debtors, (debt + amount, d))
    return plan
