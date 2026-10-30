int maxArea(int[] height) {
    int l = 0, r = height.length - 1, best = 0;
    while (l < r) {
        best = Math.max(best, (r - l) * Math.min(height[l], height[r]));
        // The shorter side limits the area; moving the taller one can only shrink width
        // without raising the limit, so move the shorter one.
        if (height[l] < height[r]) l++; else r--;
    }
    return best;
}
// O(n) time, O(1) space
