// 0/1 knapsack: each item at most once. best[c] = max value with capacity c using the
// items seen so far. Iterate capacity DOWNWARD so best[c - w] still means "without this
// item".
int knapsack(int[] weight, int[] value, int capacity) {
    int[] best = new int[capacity + 1];
    for (int i = 0; i < weight.length; i++)
        for (int c = capacity; c >= weight[i]; c--)
            best[c] = Math.max(best[c], best[c - weight[i]] + value[i]);   // skip or take
    return best[capacity];
}

// Partition equal subset sum: can the numbers be split into two equal-sum halves?
// It is 0/1 knapsack where "value" is reachability: is sum / 2 reachable?
boolean canPartition(int[] nums) {
    int total = Arrays.stream(nums).sum();
    if (total % 2 == 1) return false;
    boolean[] reachable = new boolean[total / 2 + 1];
    reachable[0] = true;
    for (int x : nums)
        for (int s = total / 2; s >= x; s--)
            reachable[s] |= reachable[s - x];
    return reachable[total / 2];
}
// knapsack([1,3,4,5], [1,4,5,7], 7) → 9 (items 3+4). canPartition([1,5,11,5]) → true. O(n
// · capacity) time, O(capacity) space — pseudo-polynomial: fine while capacity is small.
