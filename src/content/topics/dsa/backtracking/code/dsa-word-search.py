def exist(board, word):
    rows, cols = len(board), len(board[0])
    def match(i, r, c):
        if i == len(word):
            return True
        if not (0 <= r < rows and 0 <= c < cols) or board[r][c] != word[i]:
            return False
        saved, board[r][c] = board[r][c], '#'
        steps = ((1, 0), (-1, 0), (0, 1), (0, -1))
        found = any(match(i + 1, r + dr, c + dc) for dr, dc in steps)
        board[r][c] = saved
        return found
    return any(match(0, r, c) for r in range(rows) for c in range(cols))
