from collections import deque


# Tasks 0..n-1 with durations; deps = [(before, after)]. Unlimited workers run independent
# tasks in parallel. Minimum total time = the longest (critical) path, or -1 on a cycle.
def minimum_time(n: int, deps: list[tuple[int, int]], duration: list[int]) -> int:
    nxt: list[list[int]] = [[] for _ in range(n)]
    in_degree = [0] * n
    for a, b in deps:
        nxt[a].append(b)
        in_degree[b] += 1
    start = [0] * n                  # earliest start = latest prerequisite finish
    ready = deque(i for i in range(n) if in_degree[i] == 0)
    done = total = 0
    while ready:
        u = ready.popleft()
        done += 1
        finish = start[u] + duration[u]
        total = max(total, finish)
        for v in nxt[u]:
            start[v] = max(start[v], finish)   # v waits for its slowest prerequisite
            in_degree[v] -= 1
            if in_degree[v] == 0:
                ready.append(v)
    return total if done == n else -1
# durations [1,2,3,4,5], tasks 0–3 all before task 4 → 4 + 5 = 9. O(V + E).
