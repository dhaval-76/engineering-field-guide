// Shortest paths when every edge costs 0 or 1, in O(V + E): a deque instead of a heap.
// adj[u] = list of {v, w} with w ∈ {0, 1}.
int[] zeroOneBfs(List<int[]>[] adj, int source) {
    int[] dist = new int[adj.length];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[source] = 0;
    Deque<Integer> deque = new ArrayDeque<>(List.of(source));
    while (!deque.isEmpty()) {
        int u = deque.pollFirst();
        for (int[] edge : adj[u]) {
            int v = edge[0], w = edge[1];
            if (dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
                // a free edge keeps v at the same distance → front; a cost-1 edge → back
                if (w == 0) deque.offerFirst(v); else deque.offerLast(v);
            }
        }
    }
    return dist;
}
// The deque always holds distances d and d + 1 only, in order — what Dijkstra's heap
// would give, without the log factor. Example: minimum walls to remove in a grid.
