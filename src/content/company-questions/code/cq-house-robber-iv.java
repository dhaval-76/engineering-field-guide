/** Smallest "capability" (largest single amount taken) that still allows k non-adjacent houses. */
int minCapability(int[] nums, int k) {
    int lo = Arrays.stream(nums).min().getAsInt(), hi = Arrays.stream(nums).max().getAsInt();
    while (lo < hi) {
        int cap = lo + (hi - lo) / 2;
        if (canRob(nums, k, cap)) hi = cap;
        else lo = cap + 1;
    }
    return lo;
}

boolean canRob(int[] nums, int k, int cap) {
    int taken = 0;
    for (int i = 0; i < nums.length && taken < k; i++) {
        if (nums[i] <= cap) { // take it greedily and skip its neighbour
            taken++;
            i++;
        }
    }
    return taken >= k;
}
