private static final int WHITE = 0, GREY = 1, BLACK = 2;

/** Topological order of a directed graph, or null if it has a cycle. */
List<Integer> topoOrder(List<List<Integer>> adj) {
    int[] color = new int[adj.size()];
    List<Integer> finished = new ArrayList<>();
    for (int u = 0; u < adj.size(); u++)
        if (color[u] == WHITE && !dfs(u, adj, color, finished)) return null;
    Collections.reverse(finished);                  // reverse post-order
    return finished;
}

private boolean dfs(int u, List<List<Integer>> adj, int[] color, List<Integer> finished) {
    color[u] = GREY;                                // on the current path
    for (int v : adj.get(u)) {
        if (color[v] == GREY) return false;         // back edge → cycle
        if (color[v] == WHITE && !dfs(v, adj, color, finished)) return false;
    }
    color[u] = BLACK;
    finished.add(u);                                // all descendants are done
    return true;
}
