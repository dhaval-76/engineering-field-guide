function shortestPath(grid) {
  const rows = grid.length, cols = grid[0].length;
  if (grid[0][0] === '#' || grid[rows - 1][cols - 1] === '#') return -1;
  const seen = grid.map((row) => row.map(() => false));
  const queue = [[0, 0, 0]];
  seen[0][0] = true;
  // index instead of shift(): O(1)
  for (let head = 0; head < queue.length; head++) {
    const [r, c, d] = queue[head];
    if (r === rows - 1 && c === cols - 1) return d;
    for (const [dr, dc] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
      const nr = r + dr, nc = c + dc;
      if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || seen[nr][nc] || grid[nr][nc] === '#') continue;
      seen[nr][nc] = true;
      queue.push([nr, nc, d + 1]);
    }
  }
  return -1;
}
