function solveNQueens(n) {
  const cols = new Set(), diag = new Set(), anti = new Set(), queens = [], out = [];
  const place = (row) => {
    if (row === n) {
      out.push(queens.map((c) => '.'.repeat(c) + 'Q' + '.'.repeat(n - c - 1)));
      return;
    }
    for (let c = 0; c < n; c++) {
      if (cols.has(c) || diag.has(row - c) || anti.has(row + c)) continue;
      cols.add(c); diag.add(row - c); anti.add(row + c); queens.push(c);
      place(row + 1);
      cols.delete(c); diag.delete(row - c); anti.delete(row + c); queens.pop();
    }
  };
  place(0);
  return out;
}
