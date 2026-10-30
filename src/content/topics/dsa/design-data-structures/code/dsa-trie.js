class Trie {
  root = { children: new Map(), isWord: false };
  insert(word) {
    let n = this.root;
    for (const c of word) {
      if (!n.children.has(c)) n.children.set(c, { children: new Map(), isWord: false });
      n = n.children.get(c);
    }
    n.isWord = true;
  }
  #walk(s) {
    let n = this.root;
    for (const c of s) {
      n = n.children.get(c);
      if (!n) return null;
    }
    return n;
  }
  search(word) { return this.#walk(word)?.isWord ?? false; }
  startsWith(prefix) { return this.#walk(prefix) !== null; }
}
