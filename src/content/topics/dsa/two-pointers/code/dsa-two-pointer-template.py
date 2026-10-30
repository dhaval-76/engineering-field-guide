def pair_with_sum(sorted_nums, target):
    l, r = 0, len(sorted_nums) - 1
    while l < r:
        s = sorted_nums[l] + sorted_nums[r]
        if s == target:
            return [l + 1, r + 1]
        if s < target:
            l += 1
        else:
            r -= 1
    return []

def keep_if(nums, keep):
    write = 0
    for x in nums:
        if keep(x):
            nums[write] = x
            write += 1
    return write
