from functools import cache

def rob_memo(nums):
    @cache
    def best(i):
        if i < 0:
            return 0
        return max(best(i - 1), nums[i] + best(i - 2))
    return best(len(nums) - 1)

def rob(nums):
    prev = cur = 0
    for x in nums:
        prev, cur = cur, max(cur, prev + x)
    return cur
