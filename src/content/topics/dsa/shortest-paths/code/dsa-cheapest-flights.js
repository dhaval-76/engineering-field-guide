function findCheapestPrice(n, flights, src, dst, k) {
  let cost = new Array(n).fill(Infinity);
  cost[src] = 0;
  for (let round = 0; round <= k; round++) {
    const next = [...cost];
    for (const [u, v, p] of flights) if (cost[u] + p < next[v]) next[v] = cost[u] + p;
    cost = next;
  }
  return cost[dst] === Infinity ? -1 : cost[dst];
}
