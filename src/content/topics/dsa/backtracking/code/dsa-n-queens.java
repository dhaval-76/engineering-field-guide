int totalNQueens(int n) {
    return place(0, n, new HashSet<>(), new HashSet<>(), new HashSet<>());
}

private int place(int row, int n,
                  Set<Integer> cols, Set<Integer> diag, Set<Integer> anti) {
    // all rows filled: one solution
    if (row == n) return 1;
    int count = 0;
    for (int c = 0; c < n; c++) {
        // attacked
        if (cols.contains(c)) continue;
        if (diag.contains(row - c) || anti.contains(row + c)) continue;
        cols.add(c); diag.add(row - c); anti.add(row + c);
        count += place(row + 1, n, cols, diag, anti);
        cols.remove(c); diag.remove(row - c); anti.remove(row + c);
    }
    return count;
}
// n = 4 → 2, n = 8 → 92
