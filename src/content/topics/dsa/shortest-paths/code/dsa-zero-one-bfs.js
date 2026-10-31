// Shortest paths when every edge costs 0 or 1, in O(V + E): a deque instead of a heap.
// adj[u] = array of [v, w] with w ∈ {0, 1}.
function zeroOneBfs(adj, source) {
  const dist = new Array(adj.length).fill(Infinity);
  dist[source] = 0;
  const deque = [source];
  while (deque.length > 0) {
    const u = deque.shift(); // O(n) in JS arrays — use a real deque for large graphs
    for (const [v, w] of adj[u]) {
      if (dist[u] + w < dist[v]) {
        dist[v] = dist[u] + w;
        // a free edge keeps v at the same distance → front; a cost-1 edge → back
        if (w === 0) deque.unshift(v);
        else deque.push(v);
      }
    }
  }
  return dist;
}
// The deque always holds distances d and d + 1 only, in order — what Dijkstra's heap
// would give, without the log factor. Example: minimum walls to remove in a grid.
