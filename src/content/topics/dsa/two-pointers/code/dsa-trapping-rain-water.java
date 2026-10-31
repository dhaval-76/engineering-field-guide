// Water above bar i = min(tallest bar on its left, tallest on its right) − height[i].
int trap(int[] height) {
    int l = 0, r = height.length - 1, leftMax = 0, rightMax = 0, water = 0;
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
