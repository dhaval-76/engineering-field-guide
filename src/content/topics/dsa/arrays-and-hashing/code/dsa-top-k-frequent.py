from collections import Counter


# The k most frequent values in O(n): count, then bucket values by their frequency.
def top_k_frequent(nums: list[int], k: int) -> list[int]:
    freq = Counter(nums)
    buckets: list[list[int]] = [[] for _ in range(len(nums) + 1)]  # index = frequency
    for value, f in freq.items():
        buckets[f].append(value)
    out: list[int] = []
    for f in range(len(nums), 0, -1):
        for value in buckets[f]:
            if len(out) == k:
                return out
            out.append(value)
    return out
# [1,1,1,2,2,3], k = 2 → [1, 2]. O(n) time and space. Counter(nums).most_common(k) uses a
# heap.
