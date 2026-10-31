class MedianFinder {
    // max-heap
    private final PriorityQueue<Integer> low =
            new PriorityQueue<>(Comparator.reverseOrder());
    // min-heap
    private final PriorityQueue<Integer> high = new PriorityQueue<>();

    void addNum(int x) {
        low.offer(x);
        // the largest of the low half moves up
        high.offer(low.poll());
        if (high.size() > low.size()) low.offer(high.poll());   // keep low ≥ high in size
    }

    double findMedian() {
        if (low.size() > high.size()) return low.peek();
        return (low.peek() + (double) high.peek()) / 2;
    }
}
// add 1, 2 → 1.5; add 3 → 2. O(log n) per add, O(1) per median.
