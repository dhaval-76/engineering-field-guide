function orangesRotting(grid) {
  let queue = [], fresh = 0;
  grid.forEach((row, r) => row.forEach((v, c) => {
    if (v === 2) queue.push([r, c]);
    else if (v === 1) fresh++;
  }));
  let minutes = 0;
  while (queue.length && fresh > 0) {
    const next = [];
    for (const [r, c] of queue) {
      for (const [dr, dc] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
        const nr = r + dr, nc = c + dc;
        if (grid[nr]?.[nc] !== 1) continue;
        grid[nr][nc] = 2;
        fresh--;
        next.push([nr, nc]);
      }
    }
    queue = next;
    minutes++;
  }
  return fresh === 0 ? minutes : -1;
}
