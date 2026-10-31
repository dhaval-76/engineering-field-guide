// In-memory inverted index with BM25 ranking — the core of every search engine.
const STOP = new Set(['the', 'a', 'an', 'and', 'of', 'on', 'to', 'in']);
const K1 = 1.2, B = 0.75;

export const analyse = (text) =>
  text.toLowerCase().split(/[^\p{L}\p{N}_]+/u).filter((t) => t && !STOP.has(t))
    // crude stemming
    .map((t) => (t.endsWith('ing') && t.length > 5 ? t.slice(0, -3) : t.length > 3 ? t.replace(/s+$/, '') : t));

export class InvertedIndex {
  postings = new Map(); // term -> Map(doc -> term frequency)
  lengths = new Map();  // doc -> number of terms

  index(docId, text) {
    const terms = analyse(text);
    this.lengths.set(docId, terms.length);
    for (const t of terms) {
      if (!this.postings.has(t)) this.postings.set(t, new Map());
      const p = this.postings.get(t);
      p.set(docId, (p.get(docId) ?? 0) + 1);
    }
  }

  search(query, k = 10) {
    const n = this.lengths.size;
    const avg = [...this.lengths.values()].reduce((a, b) => a + b, 0) / Math.max(1, n);
    const scores = new Map();
    for (const term of analyse(query)) {
      const docs = this.postings.get(term) ?? new Map();
      // rare terms count more
      const idf = Math.log(1 + (n - docs.size + 0.5) / (docs.size + 0.5));
      for (const [doc, tf] of docs) {
        // saturating tf, length-normalised
        const norm = tf * (K1 + 1) / (tf + K1 * (1 - B + (B * this.lengths.get(doc)) / avg));
        scores.set(doc, (scores.get(doc) ?? 0) + idf * norm);
      }
    }
    return [...scores].sort((a, b) => b[1] - a[1]).slice(0, k);
  }
}
