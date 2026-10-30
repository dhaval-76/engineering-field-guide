function cloneGraph(node) {
  if (!node) return null;
  const copies = new Map();
  const dfs = (n) => {
    if (copies.has(n)) return copies.get(n);
    const copy = { val: n.val, neighbors: [] };
    copies.set(n, copy);                          // before visiting neighbours: handles cycles
    copy.neighbors = n.neighbors.map(dfs);
    return copy;
  };
  return dfs(node);
}
