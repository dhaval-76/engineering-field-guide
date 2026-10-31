// Land is added one cell at a time; report the number of islands after each addition.
// Re-running BFS each time is O(k · rows · cols); union-find makes each step near O(1).
function numIslandsOnline(rows, cols, additions) {
  const uf = new UnionFind(rows * cols);
  const land = new Array(rows * cols).fill(false);
  let islands = 0;
  const out = [];
  for (const [r0, c0] of additions) {
    const cell = r0 * cols + c0;
    if (!land[cell]) {
      // adding the same cell twice changes nothing
      land[cell] = true;
      islands++; // a new island of one cell…
      for (const [dr, dc] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
        const r = r0 + dr;
        const c = c0 + dc;
        if (r < 0 || r >= rows || c < 0 || c >= cols || !land[r * cols + c]) continue;
        if (uf.union(cell, r * cols + c)) islands--; // …merged with each neighbour
      }
    }
    out.push(islands);
  }
  return out;
}
// 3×3, add (0,0) (0,1) (1,2) (2,1) (1,1) → [1, 1, 2, 3, 1]. O(k · α(rows·cols)).
