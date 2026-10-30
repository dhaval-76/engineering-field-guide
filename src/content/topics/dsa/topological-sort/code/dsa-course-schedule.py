from collections import deque

def find_order(n: int, prerequisites: list[list[int]]) -> list[int]:
    nxt = [[] for _ in range(n)]
    in_degree = [0] * n
    for course, pre in prerequisites:
        nxt[pre].append(course)
        in_degree[course] += 1
    ready = deque(i for i in range(n) if in_degree[i] == 0)
    order = []
    while ready:
        course = ready.popleft()
        order.append(course)
        for c in nxt[course]:
            in_degree[c] -= 1
            if in_degree[c] == 0:
                ready.append(c)
    return order if len(order) == n else []
