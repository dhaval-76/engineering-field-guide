import heapq

def minimum_effort_path(heights):
    rows, cols = len(heights), len(heights[0])
    best = {(0, 0): 0}
    heap = [(0, 0, 0)]
    while heap:
        e, r, c = heapq.heappop(heap)
        if (r, c) == (rows - 1, cols - 1):
            return e
        if e > best[(r, c)]:
            continue
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols:
                ne = max(e, abs(heights[nr][nc] - heights[r][c]))
                if ne < best.get((nr, nc), float('inf')):
                    best[(nr, nc)] = ne
                    heapq.heappush(heap, (ne, nr, nc))
    return 0
