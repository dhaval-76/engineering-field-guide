// Number of subarrays with exactly k distinct values = atMost(k) − atMost(k − 1).
function subarraysWithKDistinct(nums, k) {
  return atMost(nums, k) - atMost(nums, k - 1);
}

// Subarrays with at most k distinct values: a normal variable window.
function atMost(nums, k) {
  const count = new Map();
  let left = 0;
  let total = 0;
  for (let right = 0; right < nums.length; right++) {
    count.set(nums[right], (count.get(nums[right]) ?? 0) + 1);
    while (count.size > k) {
      // too many distinct: shrink
      const n = count.get(nums[left]) - 1;
      if (n === 0) count.delete(nums[left]);
      else count.set(nums[left], n);
      left++;
    }
    total += right - left + 1; // every subarray ending at right and starting ≥ left
  }
  return total;
}
// [1,2,1,2,3], k = 2 → 7. O(n).
