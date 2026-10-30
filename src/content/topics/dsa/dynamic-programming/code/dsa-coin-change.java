/** Fewest coins to make `amount` (each coin unlimited), or -1. */
int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, Integer.MAX_VALUE);
    dp[0] = 0;
    for (int a = 1; a <= amount; a++)
        for (int c : coins)
            if (c <= a && dp[a - c] != Integer.MAX_VALUE)
                dp[a] = Math.min(dp[a], dp[a - c] + 1);
    return dp[amount] == Integer.MAX_VALUE ? -1 : dp[amount];
}

/** Number of combinations (order doesn't matter) that make `amount`. */
long countWays(int[] coins, int amount) {
    long[] ways = new long[amount + 1];
    ways[0] = 1;
    for (int c : coins)                         // coins OUTER: each combination counted once
        for (int a = c; a <= amount; a++)
            ways[a] += ways[a - c];
    return ways[amount];
}
// Swapping the loops (amounts outer) would count ordered sequences (permutations) instead.
// O(amount × coins) time, O(amount) space.
