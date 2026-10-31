// All-pairs shortest paths in O(V³). w[i][j] = edge weight, 0 on the diagonal, Infinity
// if none. After step k, dist[i][j] is the shortest path using only nodes 0..k as
// intermediate stops.
function floydWarshall(w) {
  const n = w.length;
  const dist = w.map((row) => [...row]);
  for (let k = 0; k < n; k++) {
    // k must be the OUTER loop
    for (let i = 0; i < n; i++) {
      for (let j = 0; j < n; j++) {
        const viaK = dist[i][k] + dist[k][j];
        if (viaK < dist[i][j]) dist[i][j] = viaK;
      }
    }
  }
  return dist; // dist[i][i] < 0 afterwards ⇒ i is on a negative cycle
}
// Fine for V up to a few hundred (400³ = 64 million steps). Handles negative edges.
