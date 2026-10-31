class UnionFind {
    private final int[] parent, size;
    private int components;

    UnionFind(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
        components = n;
    }

    int find(int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        // path compression
        while (parent[x] != root) { int next = parent[x]; parent[x] = root; x = next; }
        return root;
    }

    /** Returns false if a and b were already in the same set. */
    boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        // attach the smaller tree under the larger
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        return true;
    }

    boolean connected(int a, int b) { return find(a) == find(b); }
    int components() { return components; }
}
