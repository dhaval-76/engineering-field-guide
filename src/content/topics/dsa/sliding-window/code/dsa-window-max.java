int[] maxSlidingWindow(int[] nums, int k) {
    int[] out = new int[nums.length - k + 1];
    // indices; their values strictly decrease
    Deque<Integer> dq = new ArrayDeque<>();
    for (int i = 0; i < nums.length; i++) {
        // left the window
        if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
        // dominated by nums[i]
        while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) dq.pollLast();
        dq.offerLast(i);
        // front = max
        if (i >= k - 1) out[i - k + 1] = nums[dq.peekFirst()];
    }
    return out;
}
// [1,3,-1,-3,5,3,6,7], k = 3 → [3,3,5,5,6,7]. O(n): each index is pushed and popped once.
