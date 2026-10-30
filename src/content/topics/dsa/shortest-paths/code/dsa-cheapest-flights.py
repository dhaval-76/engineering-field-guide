def find_cheapest_price(n, flights, src, dst, k):
    INF = float('inf')
    cost = [INF] * n
    cost[src] = 0
    for _ in range(k + 1):
        nxt = cost[:]
        for u, v, p in flights:
            if cost[u] + p < nxt[v]:
                nxt[v] = cost[u] + p
        cost = nxt
    return -1 if cost[dst] == INF else cost[dst]
