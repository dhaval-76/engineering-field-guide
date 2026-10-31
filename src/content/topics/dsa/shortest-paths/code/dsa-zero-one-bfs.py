from collections import deque


# Shortest paths when every edge costs 0 or 1, in O(V + E): a deque instead of a heap.
# adj[u] = list of (v, w) with w ∈ {0, 1}.
def zero_one_bfs(adj: list[list[tuple[int, int]]], source: int) -> list[float]:
    dist = [float("inf")] * len(adj)
    dist[source] = 0
    dq = deque([source])
    while dq:
        u = dq.popleft()
        for v, w in adj[u]:
            if dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
                # a free edge keeps v at the same distance → front; a cost-1 edge → back
                if w == 0:
                    dq.appendleft(v)
                else:
                    dq.append(v)
    return dist
# The deque always holds distances d and d + 1 only, in order — what Dijkstra's heap
# would give, without the log factor. Example: minimum walls to remove in a grid.
