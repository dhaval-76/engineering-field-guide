def subsets_by_mask(nums):
    n = len(nums)
    return [[nums[i] for i in range(n) if mask >> i & 1] for mask in range(1 << n)]

def submasks(mask):
    sub = mask
    while sub:
        yield sub
        sub = (sub - 1) & mask
