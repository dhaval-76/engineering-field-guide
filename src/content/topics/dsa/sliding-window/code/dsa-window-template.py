def max_sum_window(nums, k):
    total, best = 0, float('-inf')
    for right, x in enumerate(nums):
        total += x
        if right >= k:
            total -= nums[right - k]
        if right >= k - 1:
            best = max(best, total)
    return best

def longest_with_sum_at_most(nums, limit):
    total = left = best = 0
    for right, x in enumerate(nums):
        total += x
        while total > limit:
            total -= nums[left]
            left += 1
        best = max(best, right - left + 1)
    return best
