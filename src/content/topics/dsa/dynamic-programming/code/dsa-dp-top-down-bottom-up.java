// House robber: maximum sum of non-adjacent houses.
// State: best(i) = most money from houses [0..i]. Transition: skip i, or rob i + best(i − 2).

/** 1. Top-down: recursion + memo. */
int robMemo(int[] nums) {
    Integer[] memo = new Integer[nums.length];
    return best(nums, nums.length - 1, memo);
}
private int best(int[] nums, int i, Integer[] memo) {
    if (i < 0) return 0;
    if (memo[i] != null) return memo[i];
    return memo[i] = Math.max(best(nums, i - 1, memo), nums[i] + best(nums, i - 2, memo));
}

/** 2. Bottom-up table: dp[i + 1] = best for the first i + 1 houses. */
int robTable(int[] nums) {
    int[] dp = new int[nums.length + 1];
    for (int i = 0; i < nums.length; i++)
        dp[i + 1] = Math.max(dp[i], nums[i] + (i >= 1 ? dp[i - 1] : 0));
    return dp[nums.length];
}

/** 3. Space-optimised: only the last two values are ever read. */
int rob(int[] nums) {
    int prev = 0, cur = 0;                     // best up to i − 2, best up to i − 1
    for (int x : nums) {
        int next = Math.max(cur, prev + x);
        prev = cur;
        cur = next;
    }
    return cur;
}
// [2,7,9,3,1] → 12 (2 + 9 + 1). O(n) time; O(1) space in version 3.
