// Offline batch job: cosine similarity from co-occurrence; keep the top N neighbours per
// item.
export function itemNeighbours(interactions, topN = 50) {
  const usersOf = new Map();
  const co = new Map();
  for (const items of interactions.values())
    for (const i of items) {
      usersOf.set(i, (usersOf.get(i) ?? 0) + 1);
      if (!co.has(i)) co.set(i, new Map());
      for (const j of items) if (i !== j) co.get(i).set(j, (co.get(i).get(j) ?? 0) + 1);
    }
  const out = new Map();
  for (const [i, js] of co)
    out.set(
      i,
      [...js]
        .map(([j, c]) => ({ item: j, sim: c / Math.sqrt(usersOf.get(i) * usersOf.get(j)) }))
        .sort((a, b) => b.sim - a.sim || (a.item < b.item ? -1 : 1))
        .slice(0, topN),
    );
  return out;
}

// Online: score unseen items by summed similarity to the user's history.
export function recommend(history, neighbours, k) {
  const score = new Map();
  for (const i of history)
    for (const { item, sim } of neighbours.get(i) ?? []) if (!history.has(item)) score.set(item, (score.get(item) ?? 0) + sim);
  const r = (x) => Math.round(x * 1e12) / 1e12;
  return [...score]
    .sort((a, b) => r(b[1]) - r(a[1]) || (a[0] < b[0] ? -1 : 1))
    .slice(0, k)
    .map(([item]) => item);
}
