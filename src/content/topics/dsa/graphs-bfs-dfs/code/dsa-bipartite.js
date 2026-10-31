// Can the nodes be split into two groups with every edge going between the groups?
// BFS-colour each component; an edge joining two nodes of the same colour means an odd
// cycle.
function isBipartite(graph) {
  const color = new Array(graph.length).fill(0); // 0 = unseen, 1 / −1 = the two groups
  for (let start = 0; start < graph.length; start++) {
    if (color[start] !== 0) continue; // graphs may be disconnected
    color[start] = 1;
    const queue = [start];
    for (let head = 0; head < queue.length; head++) {
      const u = queue[head];
      for (const v of graph[u]) {
        if (color[v] === color[u]) return false;
        if (color[v] === 0) {
          color[v] = -color[u];
          queue.push(v);
        }
      }
    }
  }
  return true;
}
// [[1,3],[0,2],[1,3],[0,2]] → true (a 4-cycle); [[1,2,3],[0,2],[0,1,3],[0,2]] → false.
