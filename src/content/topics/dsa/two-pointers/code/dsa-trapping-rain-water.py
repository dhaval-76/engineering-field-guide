# Water above bar i = min(tallest bar on its left, tallest on its right) − height[i].
def trap(height: list[int]) -> int:
    l, r = 0, len(height) - 1
    left_max = right_max = water = 0
    while l < r:
        if height[l] < height[r]:
            # Right side has a bar ≥ height[l], so left_max alone decides bar l's water.
            left_max = max(left_max, height[l])
            water += left_max - height[l]
            l += 1
        else:
            right_max = max(right_max, height[r])
            water += right_max - height[r]
            r -= 1
    return water
# [0,1,0,2,1,0,1,3,2,1,2,1] → 6. O(n) time, O(1) space.
