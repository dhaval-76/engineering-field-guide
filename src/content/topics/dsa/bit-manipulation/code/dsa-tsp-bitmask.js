// Shortest round trip visiting every city once (travelling salesman), for small n (≤
// ~16). dp[mask][last] = cheapest path that starts at city 0, visits exactly the cities
// in mask, and ends at last. 2^n · n states, n transitions each → O(2^n · n²) instead of
// O(n!).
function shortestTour(dist) {
  const n = dist.length;
  const full = (1 << n) - 1;
  const dp = Array.from({ length: 1 << n }, () => new Array(n).fill(Infinity));
  dp[1][0] = 0; // only city 0 visited, standing at 0
  for (let mask = 1; mask <= full; mask++) {
    for (let last = 0; last < n; last++) {
      if (dp[mask][last] === Infinity) continue;
      for (let next = 0; next < n; next++) {
        if (mask & (1 << next)) continue; // already visited
        const to = mask | (1 << next);
        dp[to][next] = Math.min(dp[to][next], dp[mask][last] + dist[last][next]);
      }
    }
  }
  let best = Infinity;
  for (let last = 0; last < n; last++) {
    best = Math.min(best, dp[full][last] + dist[last][0]); // close the loop to city 0
  }
  return best;
}
// n = 16: 2^16 · 16² ≈ 17 million steps, versus 15! ≈ 1.3 trillion tours to try.
