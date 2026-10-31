// Tasks 0..n-1 with durations; deps[i] = {before, after}. Unlimited workers run
// independent tasks in parallel. Minimum total time = the longest (critical) path through
// the DAG, or -1 on a cycle.
int minimumTime(int n, int[][] deps, int[] duration) {
    List<List<Integer>> next = new ArrayList<>();
    for (int i = 0; i < n; i++) next.add(new ArrayList<>());
    int[] inDegree = new int[n];
    for (int[] d : deps) { next.get(d[0]).add(d[1]); inDegree[d[1]]++; }
    int[] finish = new int[n];        // earliest finish time of each task
    // earliest start = latest finish among its prerequisites
    int[] start = new int[n];
    Deque<Integer> ready = new ArrayDeque<>();
    for (int i = 0; i < n; i++) if (inDegree[i] == 0) ready.offer(i);
    int done = 0, total = 0;
    while (!ready.isEmpty()) {
        int u = ready.poll();
        done++;
        finish[u] = start[u] + duration[u];
        total = Math.max(total, finish[u]);
        for (int v : next.get(u)) {
            // v waits for its slowest prerequisite
            start[v] = Math.max(start[v], finish[u]);
            if (--inDegree[v] == 0) ready.offer(v);
        }
    }
    return done == n ? total : -1;
}
// durations [1,2,3,4,5], tasks 0–3 all before task 4 → 4 + 5 = 9. O(V + E).
