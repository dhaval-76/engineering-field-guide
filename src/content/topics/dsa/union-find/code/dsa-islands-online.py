# Land is added one cell at a time; report the number of islands after each addition.
# Re-running BFS each time is O(k · rows · cols); union-find makes each step near O(1).
def num_islands_online(rows: int, cols: int,
                       additions: list[tuple[int, int]]) -> list[int]:
    uf = UnionFind(rows * cols)
    land = [False] * (rows * cols)
    islands = 0
    out = []
    for r0, c0 in additions:
        cell = r0 * cols + c0
        if not land[cell]:                    # adding the same cell twice changes nothing
            land[cell] = True
            islands += 1                      # a new island of one cell…
            for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                r, c = r0 + dr, c0 + dc
                if 0 <= r < rows and 0 <= c < cols and land[r * cols + c]:
                    if uf.union(cell, r * cols + c):
                        islands -= 1          # …merged with each neighbour
        out.append(islands)
    return out
# 3×3, add (0,0) (0,1) (1,2) (2,1) (1,1) → [1, 1, 2, 3, 1]. O(k · α(rows·cols)).
