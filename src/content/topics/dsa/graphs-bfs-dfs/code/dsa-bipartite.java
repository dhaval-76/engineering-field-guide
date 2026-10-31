// Can the nodes be split into two groups with every edge going between the groups?
// BFS-colour each component; an edge joining two nodes of the same colour means an odd
// cycle.
boolean isBipartite(int[][] graph) {               // graph[u] = neighbours of u
    int[] color = new int[graph.length];           // 0 = unseen, 1 / −1 = the two groups
    for (int start = 0; start < graph.length; start++) {
        if (color[start] != 0) continue;           // graphs may be disconnected
        color[start] = 1;
        Deque<Integer> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : graph[u]) {
                if (color[v] == color[u]) return false;
                if (color[v] == 0) { color[v] = -color[u]; queue.offer(v); }
            }
        }
    }
    return true;
}
// [[1,3],[0,2],[1,3],[0,2]] → true (a 4-cycle); [[1,2,3],[0,2],[0,1,3],[0,2]] → false.
// O(V + E).
