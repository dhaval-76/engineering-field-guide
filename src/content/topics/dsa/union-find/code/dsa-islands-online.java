// Land is added one cell at a time; report the number of islands after each addition.
// Re-running BFS each time is O(k · rows · cols); union-find makes each step near O(1).
List<Integer> numIslandsOnline(int rows, int cols, int[][] additions) {
    UnionFind uf = new UnionFind(rows * cols);
    boolean[] land = new boolean[rows * cols];
    int islands = 0;
    List<Integer> out = new ArrayList<>();
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int[] a : additions) {
        int cell = a[0] * cols + a[1];
        // adding the same cell twice changes nothing
        if (!land[cell]) {
            land[cell] = true;
            islands++;                           // a new island of one cell…
            for (int[] d : dirs) {
                int r = a[0] + d[0], c = a[1] + d[1];
                if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
                if (!land[r * cols + c]) continue;
                // …merged with each neighbour
                if (uf.union(cell, r * cols + c)) islands--;
            }
        }
        out.add(islands);
    }
    return out;
}
// 3×3, add (0,0) (0,1) (1,2) (2,1) (1,1) → [1, 1, 2, 3, 1]. O(k · α(rows·cols)).
