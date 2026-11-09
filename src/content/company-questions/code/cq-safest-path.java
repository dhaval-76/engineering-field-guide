int maximumSafeness(List<List<Integer>> grid) {
    int n = grid.size();
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    int[][] dist = new int[n][n];
    Deque<int[]> queue = new ArrayDeque<>();
    for (int r = 0; r < n; r++) {
        for (int c = 0; c < n; c++) {
            dist[r][c] = grid.get(r).get(c) == 1 ? 0 : -1;
            if (dist[r][c] == 0) queue.add(new int[] {r, c});
        }
    }
    while (!queue.isEmpty()) { // multi-source BFS: distance to the nearest thief
        int[] p = queue.poll();
        for (int[] d : dirs) {
            int r = p[0] + d[0], c = p[1] + d[1];
            if (r >= 0 && c >= 0 && r < n && c < n && dist[r][c] < 0) {
                dist[r][c] = dist[p[0]][p[1]] + 1;
                queue.add(new int[] {r, c});
            }
        }
    }
    // Widest path: always extend the cell with the largest distance.
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> b[2] - a[2]);
    boolean[][] seen = new boolean[n][n];
    heap.add(new int[] {0, 0, dist[0][0]});
    seen[0][0] = true;
    while (true) {
        int[] p = heap.poll();
        if (p[0] == n - 1 && p[1] == n - 1) return p[2];
        for (int[] d : dirs) {
            int r = p[0] + d[0], c = p[1] + d[1];
            if (r >= 0 && c >= 0 && r < n && c < n && !seen[r][c]) {
                seen[r][c] = true;
                heap.add(new int[] {r, c, Math.min(p[2], dist[r][c])});
            }
        }
    }
}
