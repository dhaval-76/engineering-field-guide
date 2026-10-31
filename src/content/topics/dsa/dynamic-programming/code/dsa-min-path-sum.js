// Cheapest path from the top-left to the bottom-right of a grid, moving only right or
// down. dp[c] holds the best cost to reach the current row's cell c (one row of memory).
function minPathSum(grid) {
  const cols = grid[0].length;
  const dp = new Array(cols).fill(Infinity);
  dp[0] = 0;
  for (const row of grid) {
    dp[0] += row[0]; // only from above
    // above or left
    for (let c = 1; c < cols; c++) dp[c] = Math.min(dp[c], dp[c - 1]) + row[c];
  }
  return dp[cols - 1];
}
// [[1,3,1],[1,5,1],[4,2,1]] → 7 (1→3→1→1→1). O(rows · cols) time, O(cols) space.
