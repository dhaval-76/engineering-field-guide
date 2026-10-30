/** A tree of n nodes (1..n) plus one extra edge. Return the edge that, if removed, leaves a tree. */
int[] findRedundantConnection(int[][] edges) {
    UnionFind uf = new UnionFind(edges.length + 1);
    for (int[] e : edges)
        if (!uf.union(e[0], e[1])) return e;     // endpoints already connected → this edge closes a cycle
    return new int[0];
}
