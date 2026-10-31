function cloneGraph(node) {
  if (!node) return null;
  const copies = new Map();
  const dfs = (n) => {
    if (copies.has(n)) return copies.get(n);
    const copy = { val: n.val, neighbors: [] };
    // before visiting neighbours: handles cycles
    copies.set(n, copy);
    copy.neighbors = n.neighbors.map(dfs);
    return copy;
  };
  return dfs(node);
}
