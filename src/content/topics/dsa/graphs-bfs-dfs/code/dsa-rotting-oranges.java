/** 0 empty, 1 fresh, 2 rotten. Each minute rot spreads to 4 neighbours. Minutes until none fresh, or -1. */
int orangesRotting(int[][] grid) {
    Deque<int[]> queue = new ArrayDeque<>();
    int fresh = 0;
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < grid[0].length; c++) {
            // ALL sources at minute 0
            if (grid[r][c] == 2) queue.offer(new int[]{r, c});
            else if (grid[r][c] == 1) fresh++;
        }
    int minutes = 0;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!queue.isEmpty() && fresh > 0) {
        // one BFS level = one minute
        for (int i = queue.size(); i > 0; i--) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int r = cur[0] + d[0], c = cur[1] + d[1];
                if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != 1) continue;
                grid[r][c] = 2;
                fresh--;
                queue.offer(new int[]{r, c});
            }
        }
        minutes++;
    }
    return fresh == 0 ? minutes : -1;
}
