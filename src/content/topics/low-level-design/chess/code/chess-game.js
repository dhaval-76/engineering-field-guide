// uses moveGenerators from the model above; pieces are { kind, color }, squares are [file, rank]
export class Board {
  #grid = Array.from({ length: 8 }, () => new Array(8).fill(null));
  at([f, r]) { return this.#grid[f][r]; }
  put([f, r], p) { this.#grid[f][r] = p; }
  remove([f, r]) { const p = this.#grid[f][r]; this.#grid[f][r] = null; return p; }

  kingOf(color) {
    for (let f = 0; f < 8; f++) for (let r = 0; r < 8; r++) {
      const p = this.#grid[f][r];
      if (p?.kind === 'king' && p.color === color) return [f, r];
    }
    throw new Error('no king');
  }

  isAttacked([tf, tr], by) {
    for (let f = 0; f < 8; f++) for (let r = 0; r < 8; r++) {
      const p = this.#grid[f][r];
      if (p?.color === by && moveGenerators[p.kind](this, [f, r], p.color).some(([a, b]) => a === tf && b === tr)) return true;
    }
    return false;
  }
}

// A move is a Command: it knows how to apply and fully revert itself.
const makeMove = (from, to, moved, captured) => ({
  from, to, moved, captured,
  apply(b) { b.remove(from); b.put(to, moved); },
  revert(b) { b.put(from, moved); b.put(to, captured); },
});

const other = (c) => (c === 'WHITE' ? 'BLACK' : 'WHITE');

export class Game {
  turn = 'WHITE';
  #history = [];
  constructor(board) { this.board = board; }

  // Legal = the piece's own rules AND the mover's king is not left in check.
  legalMoves(from) {
    const p = this.board.at(from);
    if (!p || p.color !== this.turn) throw new Error('not your piece');
    return moveGenerators[p.kind](this.board, from, p.color)
      .map((to) => makeMove(from, to, p, this.board.at(to)))
      .filter((m) => {
        m.apply(this.board);                                       // simulate…
        const safe = !this.board.isAttacked(this.board.kingOf(this.turn), other(this.turn));
        m.revert(this.board);                                      // …and undo
        return safe;
      });
  }

  play(from, to) {
    const m = this.legalMoves(from).find((x) => x.to[0] === to[0] && x.to[1] === to[1]);
    if (!m) throw new Error('illegal move');
    m.apply(this.board);
    this.#history.push(m);
    this.turn = other(this.turn);
    return this.status();
  }

  undo() {
    const m = this.#history.pop();
    if (!m) return;
    m.revert(this.board);
    this.turn = other(this.turn);
  }

  status() {
    const inCheck = this.board.isAttacked(this.board.kingOf(this.turn), other(this.turn));
    let anyMove = false;
    for (let i = 0; i < 64 && !anyMove; i++) {
      const sq = [i % 8, Math.floor(i / 8)];
      if (this.board.at(sq)?.color === this.turn && this.legalMoves(sq).length) anyMove = true;
    }
    if (anyMove) return inCheck ? 'CHECK' : 'ACTIVE';
    return inCheck ? 'CHECKMATE' : 'STALEMATE';
  }
}
