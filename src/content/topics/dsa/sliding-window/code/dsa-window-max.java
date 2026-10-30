int[] maxSlidingWindow(int[] nums, int k) {
    int[] out = new int[nums.length - k + 1];
    Deque<Integer> dq = new ArrayDeque<>();            // indices; their values strictly decrease
    for (int i = 0; i < nums.length; i++) {
        if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();          // left the window
        while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) dq.pollLast(); // dominated by nums[i]
        dq.offerLast(i);
        if (i >= k - 1) out[i - k + 1] = nums[dq.peekFirst()];                // front = max
    }
    return out;
}
// [1,3,-1,-3,5,3,6,7], k = 3 → [3,3,5,5,6,7]. O(n): each index is pushed and popped once.
