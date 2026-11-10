/** For every node, the fewest edge reversals so it can reach all others (edges form a tree). */
int[] minEdgeReversals(int n, int[][] edges) {
    List<List<int[]>> graph = new ArrayList<>();
    for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    for (int[] e : edges) {
        graph.get(e[0]).add(new int[] {e[1], 0}); // along the edge: free
        graph.get(e[1]).add(new int[] {e[0], 1}); // against it: one reversal
    }
    int[] parent = new int[n], order = new int[n], answer = new int[n];
    Arrays.fill(parent, -1);
    boolean[] seen = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>(List.of(0));
    seen[0] = true;
    int count = 0;
    while (!stack.isEmpty()) { // iterative DFS from node 0, counting reversals needed
        int u = stack.pop();
        order[count++] = u;
        for (int[] e : graph.get(u)) {
            if (!seen[e[0]]) {
                seen[e[0]] = true;
                parent[e[0]] = u;
                answer[0] += e[1];
                stack.push(e[0]);
            }
        }
    }
    // Rerooting: moving the root across one edge only changes that edge's cost.
    for (int idx = 1; idx < n; idx++) {
        int v = order[idx], u = parent[v];
        for (int[] e : graph.get(u)) {
            if (e[0] == v) answer[v] = answer[u] + (e[1] == 0 ? 1 : -1);
        }
    }
    return answer;
}
