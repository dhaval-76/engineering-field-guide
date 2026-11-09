boolean searchMatrix(int[][] m, int target) {
    int r = 0, c = m[0].length - 1; // top-right: left is smaller, down is bigger
    while (r < m.length && c >= 0) {
        if (m[r][c] == target) return true;
        if (m[r][c] > target) c--; else r++;
    }
    return false;
}
