function buildGraph(n, edges) {
  const adj = Array.from({ length: n }, () => []);
  for (const [u, v] of edges) {
    adj[u].push(v);
    adj[v].push(u);
  }
  return adj;
}

const DIRS = [[1, 0], [-1, 0], [0, 1], [0, -1]];
const inBounds = (grid, r, c) =>
  r >= 0 && r < grid.length && c >= 0 && c < grid[0].length;
