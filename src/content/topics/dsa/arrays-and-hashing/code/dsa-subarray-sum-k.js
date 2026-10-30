function subarraySum(nums, k) {
  const prefixCount = new Map([[0, 1]]);
  let sum = 0, count = 0;
  for (const x of nums) {
    sum += x;
    count += prefixCount.get(sum - k) ?? 0;
    prefixCount.set(sum, (prefixCount.get(sum) ?? 0) + 1);
  }
  return count;
}
