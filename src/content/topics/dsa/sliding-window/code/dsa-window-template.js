function maxSumWindow(nums, k) {
  let sum = 0, best = -Infinity;
  for (let right = 0; right < nums.length; right++) {
    sum += nums[right];
    if (right >= k) sum -= nums[right - k];
    if (right >= k - 1) best = Math.max(best, sum);
  }
  return best;
}

function longestWithSumAtMost(nums, limit) {
  let sum = 0, left = 0, best = 0;
  for (let right = 0; right < nums.length; right++) {
    sum += nums[right];
    while (sum > limit) sum -= nums[left++];
    best = Math.max(best, right - left + 1);
  }
  return best;
}
