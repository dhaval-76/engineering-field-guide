int calculateMinimumHP(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    // need[c] = least power required on entering this cell; work back from the goal.
    int[] need = new int[n + 1];
    Arrays.fill(need, Integer.MAX_VALUE);
    need[n - 1] = 1;
    for (int r = m - 1; r >= 0; r--) {
        for (int c = n - 1; c >= 0; c--) {
            need[c] = Math.max(1, Math.min(need[c], need[c + 1]) - grid[r][c]);
        }
        need[n] = Integer.MAX_VALUE;
    }
    return need[0];
}
