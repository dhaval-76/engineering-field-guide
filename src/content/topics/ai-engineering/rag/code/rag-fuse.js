// Reciprocal rank fusion: score = Σ 1 / (k + rank)
export function fuse(vectorHits, keywordHits, topN, k = 60) {
  const score = new Map();
  const byId = new Map();
  for (const list of [vectorHits, keywordHits]) {
    list.forEach((h, i) => {
      score.set(h.chunkId, (score.get(h.chunkId) ?? 0) + 1 / (k + i + 1));
      if (!byId.has(h.chunkId)) byId.set(h.chunkId, h);
    });
  }
  return [...score.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, topN)
    .map(([id]) => byId.get(id));
}

export function buildPrompt(question, chunks) {
  const sources = chunks.map((h) => `<source id="${h.chunkId}">\n${h.text}\n</source>`).join('\n');
  return (
    'Answer the question using only the sources below.\n' +
    "Cite sources like [id]. If the sources don't contain the answer, say you don't know.\n\n" +
    `${sources}\n\nQuestion: ${question}`
  );
}
