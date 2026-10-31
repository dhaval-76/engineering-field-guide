import random


# Kth largest in O(n) average, in place: partition like quicksort, recurse into one side
# only.
def find_kth_largest(nums: list[int], k: int) -> int:
    target = len(nums) - k               # index of the answer in sorted order
    lo, hi = 0, len(nums) - 1
    while True:
        p = partition(nums, lo, hi, random.randint(lo, hi))
        if p == target:
            return nums[p]
        if p < target:
            lo = p + 1
        else:
            hi = p - 1


# Lomuto partition: smaller values left of the pivot's final index, which is returned.
def partition(a: list[int], lo: int, hi: int, pivot_index: int) -> int:
    pivot = a[pivot_index]
    a[pivot_index], a[hi] = a[hi], a[pivot_index]
    store = lo
    for i in range(lo, hi):
        if a[i] < pivot:
            a[i], a[store] = a[store], a[i]
            store += 1
    a[store], a[hi] = a[hi], a[store]
    return store
# [3,2,1,5,6,4], k = 2 → 5. In practice heapq.nlargest(k, nums)[-1] is simpler and fast.
