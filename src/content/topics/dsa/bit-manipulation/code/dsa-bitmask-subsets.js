function subsetsByMask(nums) {
  const n = nums.length, out = [];
  for (let mask = 0; mask < 1 << n; mask++) {
    out.push(nums.filter((_, i) => (mask >> i) & 1));
  }
  return out;
}

function* submasks(mask) {
  // every non-empty subset of mask's bits
  for (let sub = mask; sub; sub = (sub - 1) & mask) yield sub;
}
