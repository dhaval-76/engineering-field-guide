// All-pairs shortest paths in O(V³). dist[i][j] = edge weight, 0 on the diagonal, INF if
// none. After step k, dist[i][j] is the shortest path using only nodes 0..k as
// intermediate stops.
// room to add two INFs without overflow
static final long INF = Long.MAX_VALUE / 4;

long[][] floydWarshall(long[][] w) {
    int n = w.length;
    long[][] dist = new long[n][];
    for (int i = 0; i < n; i++) dist[i] = w[i].clone();
    for (int k = 0; k < n; k++)                       // k must be the OUTER loop
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long viaK = dist[i][k] + dist[k][j];
                if (viaK < dist[i][j]) dist[i][j] = viaK;
            }
    // dist[i][i] < 0 afterwards ⇒ i is on a negative cycle
    return dist;
}
// Fine for V up to a few hundred (400³ = 64 million steps). Handles negative edges.
