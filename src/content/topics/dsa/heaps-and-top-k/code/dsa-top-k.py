import heapq
from collections import Counter

def find_kth_largest(nums: list[int], k: int) -> int:
    heap = []
    for x in nums:
        heapq.heappush(heap, x)
        if len(heap) > k:
            heapq.heappop(heap)
    return heap[0]

def top_k_frequent(nums: list[int], k: int) -> list[int]:
    freq = Counter(nums)
    return heapq.nlargest(k, freq, key=freq.get)     # size-k heap internally
