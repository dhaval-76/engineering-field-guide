// Water above bar i = min(tallest bar on its left, tallest on its right) − height[i].
function trap(height) {
  let l = 0;
  let r = height.length - 1;
  let leftMax = 0;
  let rightMax = 0;
  let water = 0;
  while (l < r) {
    if (height[l] < height[r]) {
      // Right side has a bar ≥ height[l], so leftMax alone decides bar l's water.
      leftMax = Math.max(leftMax, height[l]);
      water += leftMax - height[l++];
    } else {
      rightMax = Math.max(rightMax, height[r]);
      water += rightMax - height[r--];
    }
  }
  return water;
}
// [0,1,0,2,1,0,1,3,2,1,2,1] → 6. O(n) time, O(1) space.
