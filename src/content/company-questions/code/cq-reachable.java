boolean canReach(String[][] edges, String from, String to) {
    Map<String, List<String>> graph = new HashMap<>();
    for (String[] e : edges) graph.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
    Set<String> seen = new HashSet<>(List.of(from));
    Deque<String> queue = new ArrayDeque<>(List.of(from));
    while (!queue.isEmpty()) {
        String node = queue.poll();
        if (node.equals(to)) return true;
        for (String n : graph.getOrDefault(node, List.of())) if (seen.add(n)) queue.add(n);
    }
    return false;
}
