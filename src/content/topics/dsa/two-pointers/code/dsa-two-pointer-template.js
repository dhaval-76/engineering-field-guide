function pairWithSum(sorted, target) {
  let l = 0, r = sorted.length - 1;
  while (l < r) {
    const sum = sorted[l] + sorted[r];
    if (sum === target) return [l + 1, r + 1];
    if (sum < target) l++; else r--;
  }
  return [];
}

function keepIf(nums, keep) {
  let write = 0;
  for (let read = 0; read < nums.length; read++) if (keep(nums[read])) nums[write++] = nums[read];
  return write;
}
