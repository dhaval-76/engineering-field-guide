from collections import deque


# Can the nodes be split into two groups with every edge going between the groups?
# BFS-colour each component; an edge joining two nodes of the same colour means an odd
# cycle.
def is_bipartite(graph: list[list[int]]) -> bool:
    color = [0] * len(graph)                 # 0 = unseen, 1 / −1 = the two groups
    for start in range(len(graph)):
        if color[start]:
            continue                         # graphs may be disconnected
        color[start] = 1
        queue = deque([start])
        while queue:
            u = queue.popleft()
            for v in graph[u]:
                if color[v] == color[u]:
                    return False
                if color[v] == 0:
                    color[v] = -color[u]
                    queue.append(v)
    return True
# [[1,3],[0,2],[1,3],[0,2]] → True (a 4-cycle); [[1,2,3],[0,2],[0,1,3],[0,2]] → False.
