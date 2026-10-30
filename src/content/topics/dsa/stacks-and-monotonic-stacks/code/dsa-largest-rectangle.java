int largestRectangleArea(int[] heights) {
    Deque<Integer> stack = new ArrayDeque<>();       // indices with increasing heights
    int best = 0;
    for (int i = 0; i <= heights.length; i++) {
        int h = i == heights.length ? 0 : heights[i];  // sentinel 0 flushes the stack at the end
        while (!stack.isEmpty() && h < heights[stack.peek()]) {
            int height = heights[stack.pop()];
            int left = stack.isEmpty() ? -1 : stack.peek();   // nearest shorter bar on the left
            best = Math.max(best, height * (i - left - 1));    // i is the nearest shorter on the right
        }
        stack.push(i);
    }
    return best;
}
// [2,1,5,6,2,3] → 10 (bars 5 and 6, width 2)
