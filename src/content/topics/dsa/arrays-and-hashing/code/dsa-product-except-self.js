// out[i] = product of every element except nums[i], without division, in O(n).
function productExceptSelf(nums) {
  const n = nums.length;
  const out = new Array(n);
  out[0] = 1;
  for (let i = 1; i < n; i++) out[i] = out[i - 1] * nums[i - 1]; // product left of i
  let right = 1; // product right of i
  for (let i = n - 1; i >= 0; i--) {
    out[i] *= right;
    right *= nums[i];
  }
  return out;
}
// [1,2,3,4] → [24,12,8,6]. Works with zeros (division would not). O(n) time, O(1) extra.
