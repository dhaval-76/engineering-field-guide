// 0/1 knapsack: each item at most once. best[c] = max value with capacity c using the
// items seen so far. Iterate capacity DOWNWARD so best[c - w] still means "without this
// item".
function knapsack(weight, value, capacity) {
  const best = new Array(capacity + 1).fill(0);
  for (let i = 0; i < weight.length; i++) {
    for (let c = capacity; c >= weight[i]; c--) {
      best[c] = Math.max(best[c], best[c - weight[i]] + value[i]); // skip or take
    }
  }
  return best[capacity];
}

// Partition equal subset sum: can the numbers be split into two equal-sum halves?
// It is 0/1 knapsack where "value" is reachability: is sum / 2 reachable?
function canPartition(nums) {
  const total = nums.reduce((a, b) => a + b, 0);
  if (total % 2 === 1) return false;
  const half = total / 2;
  const reachable = new Array(half + 1).fill(false);
  reachable[0] = true;
  for (const x of nums) {
    for (let s = half; s >= x; s--) reachable[s] = reachable[s] || reachable[s - x];
  }
  return reachable[half];
}
// knapsack([1,3,4,5], [1,4,5,7], 7) → 9 (items 3+4). canPartition([1,5,11,5]) → true. O(n
// · capacity) time, O(capacity) space — pseudo-polynomial: fine while capacity is small.
