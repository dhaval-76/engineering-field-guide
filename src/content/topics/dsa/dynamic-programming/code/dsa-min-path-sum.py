# Cheapest path from the top-left to the bottom-right of a grid, moving only right or
# down. dp[c] holds the best cost to reach the current row's cell c (one row of memory).
def min_path_sum(grid: list[list[int]]) -> int:
    cols = len(grid[0])
    dp = [0] + [float("inf")] * (cols - 1)
    for row in grid:
        dp[0] += row[0]                                   # only from above
        for c in range(1, cols):
            dp[c] = min(dp[c], dp[c - 1]) + row[c]        # above or left
    return dp[-1]
# [[1,3,1],[1,5,1],[4,2,1]] → 7 (1→3→1→1→1). O(rows · cols) time, O(cols) space.
