// Pieces differ only in how they generate moves, so a table of move generators replaces subclasses.
const ORTHO = [[1, 0], [-1, 0], [0, 1], [0, -1]];
const DIAG = [[1, 1], [1, -1], [-1, 1], [-1, -1]];
const onBoard = ([f, r]) => f >= 0 && f < 8 && r >= 0 && r < 8;

function slide(board, [f, r], color, dirs) {
  const out = [];
  for (const [df, dr] of dirs) {
    for (let s = [f + df, r + dr]; onBoard(s); s = [s[0] + df, s[1] + dr]) {
      const p = board.at(s);
      if (!p) out.push(s);
      else { if (p.color !== color) out.push(s); break; }
    }
  }
  return out;
}

function steps(board, [f, r], color, deltas) {
  return deltas
    .map(([df, dr]) => [f + df, r + dr])
    .filter((s) => onBoard(s) && board.at(s)?.color !== color);
}

export const moveGenerators = {
  rook: (b, sq, c) => slide(b, sq, c, ORTHO),
  bishop: (b, sq, c) => slide(b, sq, c, DIAG),
  queen: (b, sq, c) => slide(b, sq, c, [...ORTHO, ...DIAG]),
  knight: (b, sq, c) => steps(b, sq, c, [[1, 2], [2, 1], [-1, 2], [-2, 1], [1, -2], [2, -1], [-1, -2], [-2, -1]]),
  king: (b, sq, c) => steps(b, sq, c, [...ORTHO, ...DIAG]),
  pawn: (b, [f, r], c) => {
    const dir = c === 'WHITE' ? 1 : -1, start = c === 'WHITE' ? 1 : 6, out = [];
    if (onBoard([f, r + dir]) && !b.at([f, r + dir])) {
      out.push([f, r + dir]);
      if (r === start && !b.at([f, r + 2 * dir])) out.push([f, r + 2 * dir]);
    }
    for (const df of [-1, 1]) {                                   // diagonal captures only
      const s = [f + df, r + dir];
      if (onBoard(s) && b.at(s) && b.at(s).color !== c) out.push(s);
    }
    return out;                                                   // en passant & promotion handled by Game
  },
};
