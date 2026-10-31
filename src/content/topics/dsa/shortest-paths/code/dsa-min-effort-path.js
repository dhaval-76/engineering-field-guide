// uses the Heap class from Heaps and top-K
function minimumEffortPath(heights) {
  const rows = heights.length, cols = heights[0].length;
  const best = Array.from({ length: rows }, () => new Array(cols).fill(Infinity));
  best[0][0] = 0;
  const heap = new Heap((a, b) => a[0] < b[0]);
  heap.push([0, 0, 0]);
  while (heap.size) {
    const [e, r, c] = heap.pop();
    if (r === rows - 1 && c === cols - 1) return e;
    if (e > best[r][c]) continue;                  // stale entry
    for (const [nr, nc] of [[r + 1, c], [r - 1, c], [r, c + 1], [r, c - 1]]) {
      if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
      const ne = Math.max(e, Math.abs(heights[nr][nc] - heights[r][c]));
      if (ne < best[nr][nc]) {
        best[nr][nc] = ne;
        heap.push([ne, nr, nc]);
      }
    }
  }
  return 0;
}
