# out[i] = product of every element except nums[i], without division, in O(n).
def product_except_self(nums: list[int]) -> list[int]:
    n = len(nums)
    out = [1] * n
    for i in range(1, n):
        out[i] = out[i - 1] * nums[i - 1]   # product of everything left of i
    right = 1                               # product of everything right of i
    for i in range(n - 1, -1, -1):
        out[i] *= right
        right *= nums[i]
    return out
# [1,2,3,4] → [24,12,8,6]. Works with zeros (division would not). O(n) time, O(1) extra.
