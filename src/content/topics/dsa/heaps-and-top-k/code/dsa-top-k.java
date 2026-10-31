int findKthLargest(int[] nums, int k) {
    // the k largest seen so far
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    for (int x : nums) {
        minHeap.offer(x);
        // drop the smallest of them
        if (minHeap.size() > k) minHeap.poll();
    }
    // smallest of the k largest
    return minHeap.peek();
}

int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) freq.merge(x, 1, Integer::sum);
    PriorityQueue<Map.Entry<Integer, Integer>> heap =
        new PriorityQueue<>(Map.Entry.comparingByValue());       // min-heap by frequency
    for (var e : freq.entrySet()) {
        heap.offer(e);
        if (heap.size() > k) heap.poll();
    }
    return heap.stream().mapToInt(Map.Entry::getKey).toArray();
}
// O(n log k) time, O(n) for the frequency map
