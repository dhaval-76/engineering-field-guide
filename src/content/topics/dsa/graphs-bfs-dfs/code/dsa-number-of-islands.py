def num_islands(grid: list[list[str]]) -> int:
    rows, cols = len(grid), len(grid[0])
    count = 0
    for r0 in range(rows):
        for c0 in range(cols):
            if grid[r0][c0] != '1':
                continue
            count += 1
            stack = [(r0, c0)]
            grid[r0][c0] = '0'
            while stack:                         # iterative: Python's recursion limit is ~1000
                r, c = stack.pop()
                for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
                    if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == '1':
                        grid[nr][nc] = '0'
                        stack.append((nr, nc))
    return count
