/**
 * prerequisites[i] = {course, prereq}. Returns an order, or an empty array if impossible
 * (cycle).
 */
int[] findOrder(int n, int[][] prerequisites) {
    List<List<Integer>> next = new ArrayList<>();
    for (int i = 0; i < n; i++) next.add(new ArrayList<>());
    int[] inDegree = new int[n];
    for (int[] p : prerequisites) {
        next.get(p[1]).add(p[0]);                   // edge: prerequisite → course
        inDegree[p[0]]++;
    }
    Deque<Integer> ready = new ArrayDeque<>();
    for (int i = 0; i < n; i++) if (inDegree[i] == 0) ready.offer(i);

    int[] order = new int[n];
    int taken = 0;
    while (!ready.isEmpty()) {
        int course = ready.poll();
        order[taken++] = course;
        for (int c : next.get(course))
            if (--inDegree[c] == 0) ready.offer(c);  // last prerequisite done
    }
    return taken == n ? order : new int[0];           // fewer → some nodes are in a cycle
}
