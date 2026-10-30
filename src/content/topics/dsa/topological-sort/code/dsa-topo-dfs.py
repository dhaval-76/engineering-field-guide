def topo_order(adj: list[list[int]]):
    WHITE, GREY, BLACK = 0, 1, 2
    color = [WHITE] * len(adj)
    finished = []

    def dfs(u):
        color[u] = GREY
        for v in adj[u]:
            if color[v] == GREY or (color[v] == WHITE and not dfs(v)):
                return False
        color[u] = BLACK
        finished.append(u)
        return True

    for u in range(len(adj)):
        if color[u] == WHITE and not dfs(u):
            return None
    return finished[::-1]
