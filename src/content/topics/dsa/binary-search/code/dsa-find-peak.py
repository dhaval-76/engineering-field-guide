# Any index whose value is greater than its neighbours (nums[-1] = nums[n] = −∞).
# The array is NOT sorted; binary search works because, on a downhill slope, a peak
# must exist to the left, and on an uphill slope one must exist to the right.
def find_peak_element(nums: list[int]) -> int:
    lo, hi = 0, len(nums) - 1
    while lo < hi:
        mid = (lo + hi) // 2
        if nums[mid] > nums[mid + 1]:
            hi = mid          # downhill: peak at mid or left
        else:
            lo = mid + 1      # uphill: peak strictly right
    return lo
# [1,2,1,3,5,6,4] → 5 (value 6) or 1 (value 2); adjacent values must differ. O(log n).
