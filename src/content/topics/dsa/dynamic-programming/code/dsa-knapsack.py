# 0/1 knapsack: each item at most once. best[c] = max value with capacity c using the
# items seen so far. Iterate capacity DOWNWARD so best[c - w] still means "without this
# item".
def knapsack(weight: list[int], value: list[int], capacity: int) -> int:
    best = [0] * (capacity + 1)
    for w, v in zip(weight, value):
        for c in range(capacity, w - 1, -1):
            best[c] = max(best[c], best[c - w] + v)     # skip or take
    return best[capacity]


# Partition equal subset sum: can the numbers be split into two equal-sum halves?
# It is 0/1 knapsack where "value" is reachability: is sum / 2 reachable?
def can_partition(nums: list[int]) -> bool:
    total = sum(nums)
    if total % 2:
        return False
    half = total // 2
    reachable = [True] + [False] * half
    for x in nums:
        for s in range(half, x - 1, -1):
            reachable[s] = reachable[s] or reachable[s - x]
    return reachable[half]
# knapsack([1,3,4,5], [1,4,5,7], 7) → 9 (items 3+4). can_partition([1,5,11,5]) → True. O(n
# · capacity) time, O(capacity) space — pseudo-polynomial: fine while capacity is small.
