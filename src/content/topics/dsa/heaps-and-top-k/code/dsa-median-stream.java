class MedianFinder {
    private final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder()); // max-heap
    private final PriorityQueue<Integer> high = new PriorityQueue<>();                         // min-heap

    void addNum(int x) {
        low.offer(x);
        high.offer(low.poll());                       // the largest of the low half moves up
        if (high.size() > low.size()) low.offer(high.poll());   // keep low ≥ high in size
    }

    double findMedian() {
        return low.size() > high.size() ? low.peek() : (low.peek() + (double) high.peek()) / 2;
    }
}
// add 1, 2 → 1.5; add 3 → 2. O(log n) per add, O(1) per median.
