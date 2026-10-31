# Dutch national flag: sort a list of 0s, 1s and 2s in one pass, in place. Invariant: [0,
# low) are 0s · [low, mid) are 1s · [mid, high] unknown · (high, end] are 2s
def sort_colors(nums: list[int]) -> None:
    low = mid = 0
    high = len(nums) - 1
    while mid <= high:
        if nums[mid] == 0:
            nums[low], nums[mid] = nums[mid], nums[low]
            low += 1
            mid += 1
        elif nums[mid] == 1:
            mid += 1
        else:  # don't advance mid: the swapped-in value is unchecked
            nums[mid], nums[high] = nums[high], nums[mid]
            high -= 1
# [2,0,2,1,1,0] → [0,0,1,1,2,2]. O(n), one pass, O(1) space.
