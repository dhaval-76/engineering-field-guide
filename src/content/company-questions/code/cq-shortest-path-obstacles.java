int shortestPath(int[][] grid, int k) {
    int m = grid.length, n = grid[0].length;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    int[][] bestLeft = new int[m][n]; // most removals left on arrival; a state is (r, c, left)
    for (int[] row : bestLeft) Arrays.fill(row, -1);
    Deque<int[]> queue = new ArrayDeque<>();
    queue.add(new int[] {0, 0, k});
    bestLeft[0][0] = k;
    for (int steps = 0; !queue.isEmpty(); steps++) {
        for (int size = queue.size(); size > 0; size--) {
            int[] p = queue.poll();
            if (p[0] == m - 1 && p[1] == n - 1) return steps;
            for (int[] d : dirs) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r < 0 || c < 0 || r >= m || c >= n) continue;
                int left = p[2] - grid[r][c];
                if (left > bestLeft[r][c]) { // only worth revisiting with more left
                    bestLeft[r][c] = left;
                    queue.add(new int[] {r, c, left});
                }
            }
        }
    }
    return -1;
}
