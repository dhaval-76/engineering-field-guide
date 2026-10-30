from collections import Counter

def least_interval(tasks: list[str], n: int) -> int:
    counts = Counter(tasks).values()
    max_count = max(counts)
    tied = sum(1 for c in counts if c == max_count)
    return max(len(tasks), (max_count - 1) * (n + 1) + tied)
