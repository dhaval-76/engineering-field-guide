// Dijkstra / A* over an adjacency list. Uses the Heap class from Heaps and top-K.
function haversineM([lat1, lon1], [lat2, lon2]) {
  const rad = (d) => (d * Math.PI) / 180;
  const h =
    Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 2 * 6_371_000 * Math.asin(Math.sqrt(h));
}

const MAX_SPEED = 130_000 / 3600; // m/s; never overestimate → A* stays optimal

// adj: Map(node -> [[neighbour, seconds], ...]); returns { seconds, path } (Infinity, []
// if unreachable)
export function shortest(adj, coords, src, dst, astar = true) {
  const h = astar ? (u) => haversineM(coords.get(u), coords.get(dst)) / MAX_SPEED : () => 0;
  const best = new Map([[src, 0]]),
    prev = new Map();
  const heap = new Heap((a, b) => a[0] < b[0]);
  heap.push([h(src), src]);
  while (heap.size) {
    const [, u] = heap.pop();
    if (u === dst) {
      const path = [u];
      for (let x = u; prev.has(x);) path.push((x = prev.get(x)));
      return { seconds: best.get(dst), path: path.reverse() };
    }
    for (const [v, w] of adj.get(u) ?? []) {
      const d = best.get(u) + w;
      if (d < (best.get(v) ?? Infinity)) {
        best.set(v, d);
        prev.set(v, u);
        heap.push([d + h(v), v]);
      }
    }
  }
  return { seconds: Infinity, path: [] };
}
