from collections import defaultdict


# Number of subarrays with exactly k distinct values = at_most(k) − at_most(k − 1).
def subarrays_with_k_distinct(nums: list[int], k: int) -> int:
    return at_most(nums, k) - at_most(nums, k - 1)


# Subarrays with at most k distinct values: a normal variable window.
def at_most(nums: list[int], k: int) -> int:
    count: defaultdict[int, int] = defaultdict(int)
    left = total = 0
    for right, x in enumerate(nums):
        count[x] += 1
        while len(count) > k:                 # too many distinct: shrink
            count[nums[left]] -= 1
            if count[nums[left]] == 0:
                del count[nums[left]]
            left += 1
        total += right - left + 1   # every subarray ending at right and starting ≥ left
    return total
# [1,2,1,2,3], k = 2 → 7. O(n).
