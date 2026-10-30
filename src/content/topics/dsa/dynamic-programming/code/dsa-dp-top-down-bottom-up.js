function robMemo(nums) {
  const memo = new Map();
  const best = (i) => {
    if (i < 0) return 0;
    if (!memo.has(i)) memo.set(i, Math.max(best(i - 1), nums[i] + best(i - 2)));
    return memo.get(i);
  };
  return best(nums.length - 1);
}

function rob(nums) {
  let prev = 0, cur = 0;
  for (const x of nums) [prev, cur] = [cur, Math.max(cur, prev + x)];
  return cur;
}
