/** times[i] = {u, v, w}; nodes 1..n. Time for a signal from k to reach all nodes, or -1. */
int networkDelayTime(int[][] times, int n, int k) {
    List<List<int[]>> adj = new ArrayList<>();
    for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
    for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]});

    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
    heap.offer(new int[]{0, k});

    while (!heap.isEmpty()) {
        int[] top = heap.poll();
        int d = top[0], u = top[1];
        if (d > dist[u]) continue;                       // stale entry: u was settled more cheaply
        for (int[] edge : adj.get(u)) {
            int v = edge[0], nd = d + edge[1];
            if (nd < dist[v]) {                          // relax
                dist[v] = nd;
                heap.offer(new int[]{nd, v});
            }
        }
    }
    int worst = 0;
    for (int i = 1; i <= n; i++) worst = Math.max(worst, dist[i]);
    return worst == Integer.MAX_VALUE ? -1 : worst;
}
// O((V + E) log V)
