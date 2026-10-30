export class Trie {
  #root = { next: new Map(), word: false };

  insert(w) {
    let n = this.#root;
    for (const c of w) {
      if (!n.next.has(c)) n.next.set(c, { next: new Map(), word: false });
      n = n.next.get(c);
    }
    n.word = true;
  }

  complete(prefix, limit = 10) {
    let n = this.#root;
    for (const c of prefix) { n = n.next.get(c); if (!n) return []; }
    const out = [];
    const walk = (node, acc) => {
      if (out.length >= limit) return;
      if (node.word) out.push(acc);
      for (const [c, child] of [...node.next].sort(([a], [b]) => a.localeCompare(b))) walk(child, acc + c);
    };
    walk(n, prefix);
    return out;
  }
}
