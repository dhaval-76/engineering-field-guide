def merge_slots(slots: list[tuple[int, int]]) -> list[tuple[int, int]]:
    """Merge half-open [start, end) slots that overlap or touch. Input may be unsorted; not mutated."""
    out: list[list[int]] = []
    for start, end in sorted(slots):
        if out and start <= out[-1][1]:
            out[-1][1] = max(out[-1][1], end)
        else:
            out.append([start, end])
    return [(s, e) for s, e in out]
