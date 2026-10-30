from bisect import bisect_left, bisect_right

def lower_bound(nums, target):
    lo, hi = 0, len(nums)
    while lo < hi:
        mid = (lo + hi) // 2
        if nums[mid] >= target:
            hi = mid
        else:
            lo = mid + 1
    return lo                     # same as bisect_left(nums, target)

def search(nums, target):
    i = bisect_left(nums, target)
    return i if i < len(nums) and nums[i] == target else -1

def last_index(nums, target):
    i = bisect_right(nums, target) - 1
    return i if i >= 0 and nums[i] == target else -1

def first_true(lo, hi, ok):
    while lo < hi:
        mid = (lo + hi) // 2
        if ok(mid):
            hi = mid
        else:
            lo = mid + 1
    return lo
