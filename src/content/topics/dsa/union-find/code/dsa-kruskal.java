/** Minimum cost to connect all points, where cost = Manhattan distance. */
int minCostConnectPoints(int[][] points) {
    int n = points.length;
    List<int[]> edges = new ArrayList<>();                      // {cost, i, j}
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            int dx = Math.abs(points[i][0] - points[j][0]);
            int dy = Math.abs(points[i][1] - points[j][1]);
            edges.add(new int[]{dx + dy, i, j});
        }
    }
    edges.sort(Comparator.comparingInt(e -> e[0]));              // cheapest first

    UnionFind uf = new UnionFind(n);
    int total = 0, used = 0;
    for (int[] e : edges) {
        // joins two components → part of the MST
        if (uf.union(e[1], e[2])) {
            total += e[0];
            // a tree on n nodes has n − 1 edges
            if (++used == n - 1) break;
        }
    }
    return total;
}
// O(E log E) with E = n²/2; Prim's with a heap is better for dense complete graphs.
