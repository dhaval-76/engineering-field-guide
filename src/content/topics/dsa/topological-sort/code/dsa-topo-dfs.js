function topoOrder(adj) {
  const color = new Array(adj.length).fill(0);   // 0 white, 1 grey, 2 black
  const finished = [];
  const dfs = (u) => {
    color[u] = 1;
    for (const v of adj[u]) {
      if (color[v] === 1) return false;
      if (color[v] === 0 && !dfs(v)) return false;
    }
    color[u] = 2;
    finished.push(u);
    return true;
  };
  for (let u = 0; u < adj.length; u++) if (color[u] === 0 && !dfs(u)) return null;
  return finished.reverse();
}
