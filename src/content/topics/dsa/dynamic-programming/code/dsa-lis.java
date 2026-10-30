/** O(n²): dp[i] = length of the longest increasing subsequence ENDING at i. */
int lengthOfLISQuadratic(int[] nums) {
    int[] dp = new int[nums.length];
    int best = 0;
    for (int i = 0; i < nums.length; i++) {
        dp[i] = 1;
        for (int j = 0; j < i; j++)
            if (nums[j] < nums[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
        best = Math.max(best, dp[i]);
    }
    return best;
}

/** O(n log n): tails[k] = smallest possible tail of an increasing subsequence of length k + 1. */
int lengthOfLIS(int[] nums) {
    int[] tails = new int[nums.length];
    int size = 0;
    for (int x : nums) {
        int lo = 0, hi = size;                       // first tail ≥ x (lower bound)
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (tails[mid] >= x) hi = mid; else lo = mid + 1;
        }
        tails[lo] = x;                               // replace, or extend when lo == size
        if (lo == size) size++;
    }
    return size;
}
// [10,9,2,5,3,7,101,18] → 4 (e.g. 2,3,7,18)
