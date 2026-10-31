function exist(board, word) {
  const rows = board.length, cols = board[0].length;
  const match = (i, r, c) => {
    if (i === word.length) return true;
    if (r < 0 || r >= rows || c < 0 || c >= cols || board[r][c] !== word[i]) return false;
    const saved = board[r][c];
    board[r][c] = '#';                             // mark visited on this path
    const found =
      match(i + 1, r + 1, c) ||
      match(i + 1, r - 1, c) ||
      match(i + 1, r, c + 1) ||
      match(i + 1, r, c - 1);
    board[r][c] = saved;                           // unmark
    return found;
  };
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) if (match(0, r, c)) return true;
  }
  return false;
}
