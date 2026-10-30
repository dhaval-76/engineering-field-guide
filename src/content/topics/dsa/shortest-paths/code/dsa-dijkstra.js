// uses the Heap class from Heaps and top-K
function networkDelayTime(times, n, k) {
  const adj = Array.from({ length: n + 1 }, () => []);
  for (const [u, v, w] of times) adj[u].push([v, w]);
  const dist = new Array(n + 1).fill(Infinity);
  dist[k] = 0;
  const heap = new Heap((a, b) => a[0] < b[0]);
  heap.push([0, k]);
  while (heap.size) {
    const [d, u] = heap.pop();
    if (d > dist[u]) continue;
    for (const [v, w] of adj[u]) {
      if (d + w < dist[v]) {
        dist[v] = d + w;
        heap.push([dist[v], v]);
      }
    }
  }
  const worst = Math.max(...dist.slice(1));
  return worst === Infinity ? -1 : worst;
}
