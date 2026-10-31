/**
 * Cost of a path = largest absolute height difference between consecutive cells. Minimise
 * it.
 */
int minimumEffortPath(int[][] heights) {
    int rows = heights.length, cols = heights[0].length;
    int[][] effort = new int[rows][cols];
    for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
    effort[0][0] = 0;
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
    heap.offer(new int[]{0, 0, 0});
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!heap.isEmpty()) {
        int[] cur = heap.poll();
        int e = cur[0], r = cur[1], c = cur[2];
        if (r == rows - 1 && c == cols - 1) return e;
        if (e > effort[r][c]) continue;
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
            // max, not sum
            int ne = Math.max(e, Math.abs(heights[nr][nc] - heights[r][c]));
            if (ne < effort[nr][nc]) { effort[nr][nc] = ne; heap.offer(new int[]{ne, nr, nc}); }
        }
    }
    return 0;
}
