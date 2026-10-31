// Ranked autocomplete: every node caches its top K completions, so a query is O(prefix length).
const K = 5;
const better = (a, b) => b.score - a.score || (a.word < b.word ? -1 : a.word > b.word ? 1 : 0);

export class RankedAutocomplete {
  #root = { next: new Map(), top: [] };
  #scores = new Map();

  // Add `delta` to a phrase's score (e.g. +1 per search) and refresh the caches on its path.
  record(phrase, delta) {
    const p = phrase.trim().toLowerCase();
    const score = (this.#scores.get(p) ?? 0) + delta;
    this.#scores.set(p, score);
    const s = { word: p, score };
    let n = this.#root;
    update(n, s);
    for (const c of p) {
      if (!n.next.has(c)) n.next.set(c, { next: new Map(), top: [] });
      n = n.next.get(c);
      update(n, s);
    }
  }

  suggest(prefix) {
    let n = this.#root;
    for (const c of prefix.toLowerCase()) {
      n = n.next.get(c);
      if (!n) return [];
    }
    return n.top.map((s) => s.word);
  }
}

function update(node, s) {
  node.top = node.top.filter((x) => x.word !== s.word); // no duplicate entries for one phrase
  node.top.push(s);
  node.top.sort(better);
  if (node.top.length > K) node.top.pop();
}
