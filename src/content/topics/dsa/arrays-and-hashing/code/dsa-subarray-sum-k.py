from collections import defaultdict

def subarray_sum(nums: list[int], k: int) -> int:
    prefix_count = defaultdict(int, {0: 1})
    total = count = 0
    for x in nums:
        total += x
        count += prefix_count[total - k]
        prefix_count[total] += 1
    return count
