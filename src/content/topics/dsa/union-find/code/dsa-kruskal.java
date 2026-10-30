/** Minimum cost to connect all points, where cost = Manhattan distance. */
int minCostConnectPoints(int[][] points) {
    int n = points.length;
    List<int[]> edges = new ArrayList<>();                      // {cost, i, j}
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            edges.add(new int[]{Math.abs(points[i][0] - points[j][0]) + Math.abs(points[i][1] - points[j][1]), i, j});
    edges.sort(Comparator.comparingInt(e -> e[0]));              // cheapest first

    UnionFind uf = new UnionFind(n);
    int total = 0, used = 0;
    for (int[] e : edges) {
        if (uf.union(e[1], e[2])) {                              // joins two components → part of the MST
            total += e[0];
            if (++used == n - 1) break;                          // a tree on n nodes has n − 1 edges
        }
    }
    return total;
}
// O(E log E) with E = n²/2; Prim's with a heap is better for dense complete graphs.
