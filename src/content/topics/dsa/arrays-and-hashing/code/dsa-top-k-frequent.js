// The k most frequent values in O(n): count, then bucket values by their frequency.
function topKFrequent(nums, k) {
  const freq = new Map();
  for (const x of nums) freq.set(x, (freq.get(x) ?? 0) + 1);
  // buckets[f] = values that occur exactly f times (f is at most n)
  const buckets = Array.from({ length: nums.length + 1 }, () => []);
  for (const [value, f] of freq) buckets[f].push(value);
  const out = [];
  for (let f = nums.length; f > 0 && out.length < k; f--)
    for (const value of buckets[f]) if (out.length < k) out.push(value);
  return out;
}
// [1,1,1,2,2,3], k = 2 → [1, 2]. O(n) time and space (a heap would be O(n log k)).
