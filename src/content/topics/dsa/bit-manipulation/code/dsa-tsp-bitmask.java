// Shortest round trip visiting every city once (travelling salesman), for small n (≤
// ~16). dp[mask][last] = cheapest path that starts at city 0, visits exactly the cities
// in mask, and ends at last. 2^n · n states, n transitions each → O(2^n · n²) instead of
// O(n!).
int shortestTour(int[][] dist) {
    int n = dist.length, full = (1 << n) - 1;
    int[][] dp = new int[1 << n][n];
    for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE);
    // only city 0 visited, standing at 0
    dp[1][0] = 0;
    for (int mask = 1; mask <= full; mask++) {
        for (int last = 0; last < n; last++) {
            if (dp[mask][last] == Integer.MAX_VALUE) continue;
            for (int next = 0; next < n; next++) {
                if ((mask & (1 << next)) != 0) continue;  // already visited
                int to = mask | (1 << next);
                dp[to][next] = Math.min(dp[to][next], dp[mask][last] + dist[last][next]);
            }
        }
    }
    int best = Integer.MAX_VALUE;
    for (int last = 0; last < n; last++) {                // close the loop back to city 0
        if (dp[full][last] == Integer.MAX_VALUE) continue;
        best = Math.min(best, dp[full][last] + dist[last][0]);
    }
    return best;
}
// n = 16: 2^16 · 16² ≈ 17 million steps, versus 15! ≈ 1.3 trillion tours to try.
