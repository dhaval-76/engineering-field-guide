int numIslands(char[][] grid) {
    int count = 0;
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < grid[0].length; c++)
            if (grid[r][c] == '1') { count++; sink(grid, r, c); }   // each new land cell = new island
    return count;
}

/** Iterative flood fill: no stack overflow on a 1000×1000 all-land grid. Marks visited by sinking. */
private void sink(char[][] grid, int r0, int c0) {
    Deque<int[]> stack = new ArrayDeque<>();
    stack.push(new int[]{r0, c0});
    grid[r0][c0] = '0';
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!stack.isEmpty()) {
        int[] cur = stack.pop();
        for (int[] d : dirs) {
            int r = cur[0] + d[0], c = cur[1] + d[1];
            if (r >= 0 && r < grid.length && c >= 0 && c < grid[0].length && grid[r][c] == '1') {
                grid[r][c] = '0';
                stack.push(new int[]{r, c});
            }
        }
    }
}
// O(rows × cols). (Copy the grid first if the caller needs it unchanged.)
