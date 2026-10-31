/** Fewest steps from (0,0) to the bottom-right through '.' cells; -1 if unreachable. */
int shortestPath(char[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    if (grid[0][0] == '#' || grid[rows - 1][cols - 1] == '#') return -1;
    boolean[][] seen = new boolean[rows][cols];
    Deque<int[]> queue = new ArrayDeque<>();
    queue.offer(new int[]{0, 0, 0});
    seen[0][0] = true;                                         // mark when enqueuing
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        // first arrival = shortest
        if (cur[0] == rows - 1 && cur[1] == cols - 1) return cur[2];
        for (int[] d : dirs) {
            int r = cur[0] + d[0], c = cur[1] + d[1];
            if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
            if (seen[r][c] || grid[r][c] == '#') continue;
            seen[r][c] = true;
            queue.offer(new int[]{r, c, cur[2] + 1});
        }
    }
    return -1;
}
