# Shortest round trip visiting every city once (travelling salesman), for small n (≤ ~16).
# dp[mask][last] = cheapest path that starts at city 0, visits exactly the cities in mask,
# and ends at last. 2^n · n states, n transitions each → O(2^n · n²) instead of O(n!).
def shortest_tour(dist: list[list[int]]) -> float:
    n = len(dist)
    full = (1 << n) - 1
    inf = float("inf")
    dp = [[inf] * n for _ in range(1 << n)]
    dp[1][0] = 0                                    # only city 0 visited, standing at 0
    for mask in range(1, full + 1):
        for last in range(n):
            cost = dp[mask][last]
            if cost == inf:
                continue
            for nxt in range(n):
                if mask & (1 << nxt):
                    continue                        # already visited
                to = mask | (1 << nxt)
                if cost + dist[last][nxt] < dp[to][nxt]:
                    dp[to][nxt] = cost + dist[last][nxt]
    return min(dp[full][last] + dist[last][0] for last in range(n))
# n = 16: 2^16 · 16² ≈ 17 million steps, versus 15! ≈ 1.3 trillion tours to try.
