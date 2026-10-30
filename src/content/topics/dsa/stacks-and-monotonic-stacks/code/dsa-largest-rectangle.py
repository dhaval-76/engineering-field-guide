def largest_rectangle_area(heights: list[int]) -> int:
    stack, best = [], 0                          # indices with increasing heights
    for i, h in enumerate(heights + [0]):        # sentinel 0 flushes the stack
        while stack and h < heights[stack[-1]]:
            height = heights[stack.pop()]
            left = stack[-1] if stack else -1
            best = max(best, height * (i - left - 1))
        stack.append(i)
    return best
