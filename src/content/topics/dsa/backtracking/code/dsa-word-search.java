boolean exist(char[][] board, String word) {
    for (int r = 0; r < board.length; r++)
        for (int c = 0; c < board[0].length; c++)
            if (match(board, word, 0, r, c)) return true;
    return false;
}

private boolean match(char[][] b, String word, int i, int r, int c) {
    if (i == word.length()) return true;
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length) return false;
    if (b[r][c] != word.charAt(i)) return false;
    char saved = b[r][c];
    // mark as on the current path
    b[r][c] = '#';
    boolean found = match(b, word, i + 1, r + 1, c) || match(b, word, i + 1, r - 1, c)
                 || match(b, word, i + 1, r, c + 1) || match(b, word, i + 1, r, c - 1);
    b[r][c] = saved;                                          // unmark when backtracking
    return found;
}
// O(R·C·4^L) worst case for word length L
