# All-pairs shortest paths in O(V³). w[i][j] = edge weight, 0 on the diagonal, inf if
# none. After step k, dist[i][j] is the shortest path using only nodes 0..k as
# intermediate stops.
def floyd_warshall(w: list[list[float]]) -> list[list[float]]:
    n = len(w)
    dist = [row[:] for row in w]
    for k in range(n):                       # k must be the OUTER loop
        dk = dist[k]
        for i in range(n):
            dik = dist[i][k]
            row = dist[i]
            for j in range(n):
                if dik + dk[j] < row[j]:
                    row[j] = dik + dk[j]
    return dist                              # dist[i][i] < 0 ⇒ i is on a negative cycle
# Fine for V up to a few hundred in compiled languages; in Python, a few hundred is slow.
