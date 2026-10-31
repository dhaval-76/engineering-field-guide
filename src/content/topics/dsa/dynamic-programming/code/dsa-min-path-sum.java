// Cheapest path from the top-left to the bottom-right of a grid, moving only right or
// down. dp[c] holds the best cost to reach the current row's cell c (one row of memory).
int minPathSum(int[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    int[] dp = new int[cols];
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (r == 0 && c == 0) dp[c] = grid[0][0];
            // only from the left
            else if (r == 0) dp[c] = dp[c - 1] + grid[r][c];
            // only from above
            else if (c == 0) dp[c] = dp[c] + grid[r][c];
            // above or left
            else dp[c] = Math.min(dp[c], dp[c - 1]) + grid[r][c];
        }
    }
    return dp[cols - 1];
}
// [[1,3,1],[1,5,1],[4,2,1]] → 7 (1→3→1→1→1). O(rows · cols) time, O(cols) space.
