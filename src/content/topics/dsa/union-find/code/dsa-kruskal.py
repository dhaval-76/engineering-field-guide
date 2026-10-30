def min_cost_connect_points(points):
    n = len(points)
    edges = sorted(
        (abs(x1 - x2) + abs(y1 - y2), i, j)
        for i, (x1, y1) in enumerate(points)
        for j, (x2, y2) in enumerate(points) if i < j
    )
    uf, total, used = UnionFind(n), 0, 0
    for cost, i, j in edges:
        if uf.union(i, j):
            total += cost
            used += 1
            if used == n - 1:
                break
    return total
