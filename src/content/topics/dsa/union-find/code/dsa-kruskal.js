// uses UnionFind from above
function minCostConnectPoints(points) {
  const n = points.length, edges = [];
  for (let i = 0; i < n; i++)
    for (let j = i + 1; j < n; j++)
      edges.push([Math.abs(points[i][0] - points[j][0]) + Math.abs(points[i][1] - points[j][1]), i, j]);
  edges.sort((a, b) => a[0] - b[0]);
  const uf = new UnionFind(n);
  let total = 0, used = 0;
  for (const [cost, i, j] of edges) {
    if (uf.union(i, j)) {
      total += cost;
      if (++used === n - 1) break;                 // a spanning tree has n - 1 edges
    }
  }
  return total;
}
