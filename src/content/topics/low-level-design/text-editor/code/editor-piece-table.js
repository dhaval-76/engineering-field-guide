// Piece table: the original text is never modified; inserted text is appended to an "add"
// buffer; the document is a list of pieces pointing into either buffer. Edits only split,
// drop or insert pieces.
export class PieceTable {
  #original;
  #add = '';
  #pieces = []; // { fromAdd, start, length } — a balanced tree in real editors

  constructor(original = '') {
    this.#original = original;
    if (original) this.#pieces.push({ fromAdd: false, start: 0, length: original.length });
  }

  insert(pos, text) {
    const inserted = { fromAdd: true, start: this.#add.length, length: text.length };
    this.#add += text; // append-only: old pieces stay valid
    let offset = 0;
    for (let i = 0; i < this.#pieces.length; i++) {
      const p = this.#pieces[i];
      if (pos <= offset + p.length) {
        const split = pos - offset; // [left][inserted][right]
        const parts = [];
        if (split > 0) parts.push({ ...p, length: split });
        parts.push(inserted);
        if (split < p.length)
          parts.push({ ...p, start: p.start + split, length: p.length - split });
        this.#pieces.splice(i, 1, ...parts);
        return;
      }
      offset += p.length;
    }
    this.#pieces.push(inserted);
  }

  // Keep the parts of each piece outside [pos, pos + len); return the removed text (for
  // undo).
  delete(pos, len) {
    let removed = '';
    let offset = 0;
    const end = pos + len;
    const kept = [];
    for (const p of this.#pieces) {
      const pStart = offset,
        pEnd = offset + p.length;
      offset = pEnd;
      const from = Math.max(pos, pStart),
        to = Math.min(end, pEnd);
      if (from >= to) {
        kept.push(p);
        continue;
      }
      const buf = p.fromAdd ? this.#add : this.#original;
      removed += buf.slice(p.start + from - pStart, p.start + to - pStart);
      if (from > pStart) kept.push({ ...p, length: from - pStart });
      if (to < pEnd) kept.push({ ...p, start: p.start + to - pStart, length: pEnd - to });
    }
    this.#pieces = kept;
    return removed;
  }

  text() {
    return this.#pieces
      .map((p) => (p.fromAdd ? this.#add : this.#original).slice(p.start, p.start + p.length))
      .join('');
  }
}
