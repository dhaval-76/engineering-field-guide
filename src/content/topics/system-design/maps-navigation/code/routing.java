/**
 * Road graph in compact arrays: edges of node u are to[firstEdge[u] .. firstEdge[u+1]).
 */
record Graph(int[] firstEdge, int[] to, int[] travelMillis, double[] lat, double[] lon) {}

final class Router {
    // 130 km/h — keeps A* admissible
    private static final double MAX_SPEED_M_PER_MS = 130_000.0 / 3_600_000;

    /**
     * A* search; with heuristic = 0 it is exactly Dijkstra. Returns travel time in ms, or
     * -1.
     */
    static long shortest(Graph g, int src, int dst, boolean useHeuristic) {
        int n = g.firstEdge().length - 1;
        long[] best = new long[n];
        Arrays.fill(best, Long.MAX_VALUE);
        best[src] = 0;
        // queue entries: {priority, node}; priority = best-so-far + estimate to goal
        PriorityQueue<long[]> open = new PriorityQueue<>(Comparator.comparingLong(e -> e[0]));
        open.add(new long[]{h(g, src, dst, useHeuristic), src});

        while (!open.isEmpty()) {
            long[] e = open.poll();
            int u = (int) e[1];
            // stale entry (compare distances, not priorities)
            if (e[0] - h(g, u, dst, useHeuristic) > best[u]) continue;
            if (u == dst) return best[u];
            for (int i = g.firstEdge()[u]; i < g.firstEdge()[u + 1]; i++) {
                int v = g.to()[i];
                long d = best[u] + g.travelMillis()[i];
                if (d < best[v]) {
                    best[v] = d;
                    open.add(new long[]{d + h(g, v, dst, useHeuristic), v});
                }
            }
        }
        return -1;
    }

    /**
     * Admissible estimate: straight-line metres to the goal at the maximum possible
     * speed.
     */
    private static long h(Graph g, int u, int dst, boolean use) {
        if (!use) return 0;
        return (long) (haversineMetres(g.lat()[u], g.lon()[u], g.lat()[dst], g.lon()[dst]) / MAX_SPEED_M_PER_MS);
    }

    static double haversineMetres(double lat1, double lon1, double lat2, double lon2) {
        double r = 6_371_000, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
