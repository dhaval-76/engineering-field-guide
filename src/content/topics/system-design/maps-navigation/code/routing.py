import heapq, math

def haversine_m(a, b):
    (lat1, lon1), (lat2, lon2) = a, b
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6_371_000 * math.asin(math.sqrt(h))

MAX_SPEED = 130_000 / 3_600          # m/s; never overestimate → A* stays optimal

def shortest(adj, coords, src, dst, astar=True):
    """adj: {node: [(neighbour, seconds), ...]}; returns (seconds, path) or (inf, [])."""
    h = (lambda u: haversine_m(coords[u], coords[dst]) / MAX_SPEED) if astar else (lambda u: 0.0)
    best, prev = {src: 0.0}, {}
    heap = [(h(src), src)]
    while heap:
        _, u = heapq.heappop(heap)
        if u == dst:
            path = [u]
            while u in prev:
                u = prev[u]; path.append(u)
            return best[dst], path[::-1]
        for v, w in adj.get(u, []):
            d = best[u] + w
            if d < best.get(v, math.inf):
                best[v], prev[v] = d, u
                heapq.heappush(heap, (d + h(v), v))
    return math.inf, []
