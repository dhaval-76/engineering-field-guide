// Before trusting a greedy rule, compare it with an exact answer on many small inputs.
// Example: "largest coin first" for coin change. Returns the first amount where greedy
// uses more coins than the optimum (from DP), or -1 if none up to the limit.
function firstGreedyFailure(coins, limit) {
  const sorted = [...coins].sort((a, b) => a - b);
  const best = new Array(limit + 1).fill(Infinity); // DP: fewest coins for each amount
  best[0] = 0;
  for (let a = 1; a <= limit; a++) {
    for (const c of sorted) if (c <= a) best[a] = Math.min(best[a], best[a - c] + 1);
  }
  for (let a = 1; a <= limit; a++) {
    let left = a;
    let used = 0;
    for (let i = sorted.length - 1; i >= 0; i--) {
      // greedy: largest coin first
      used += Math.floor(left / sorted[i]);
      left %= sorted[i];
    }
    if (left === 0 && used > best[a]) return a; // greedy works, but uses more coins
  }
  return -1;
}
// [1,3,4] → 6 (greedy 4+1+1, optimal 3+3). [1,2,5,10,20,50,100,200,500,2000] → -1:
// real currencies are designed so greedy works ("canonical" coin systems).
