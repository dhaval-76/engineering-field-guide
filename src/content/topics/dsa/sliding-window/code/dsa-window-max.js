function maxSlidingWindow(nums, k) {
  const out = [], dq = [];          // indices; use a head pointer for O(1) pops from the front
  let head = 0;
  for (let i = 0; i < nums.length; i++) {
    if (head < dq.length && dq[head] <= i - k) head++;
    while (dq.length > head && nums[dq[dq.length - 1]] <= nums[i]) dq.pop();
    dq.push(i);
    if (i >= k - 1) out.push(nums[dq[head]]);
  }
  return out;
}
