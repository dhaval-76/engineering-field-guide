/** Undirected graph from an edge list: n nodes numbered 0..n-1. */
List<List<Integer>> buildGraph(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) {
        adj.get(e[0]).add(e[1]);
        adj.get(e[1]).add(e[0]);          // omit for a directed graph
    }
    return adj;
}

static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

/** Grid as an implicit graph: neighbours computed, never stored. */
boolean inBounds(int[][] grid, int r, int c) {
    return r >= 0 && r < grid.length && c >= 0 && c < grid[0].length;
}
