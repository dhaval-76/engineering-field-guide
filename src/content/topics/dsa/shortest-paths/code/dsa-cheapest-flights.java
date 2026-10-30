/** flights[i] = {from, to, price}. Cheapest price from src to dst with at most k stops (k + 1 flights). */
int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    int INF = Integer.MAX_VALUE;
    int[] cost = new int[n];
    Arrays.fill(cost, INF);
    cost[src] = 0;
    for (int round = 0; round <= k; round++) {
        int[] next = cost.clone();                        // use only the previous round's costs
        for (int[] f : flights) {
            if (cost[f[0]] == INF) continue;
            next[f[1]] = Math.min(next[f[1]], cost[f[0]] + f[2]);
        }
        cost = next;                                      // now: cheapest with ≤ round + 1 flights
    }
    return cost[dst] == INF ? -1 : cost[dst];
}
// O(k · E). Negative prices would also be fine (no negative cycles reachable).
