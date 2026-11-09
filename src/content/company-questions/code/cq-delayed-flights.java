/** dependencies: {a, b} means b can't leave until a arrives. */
Set<String> affectedFlights(String[][] dependencies, List<String> delayed) {
    Map<String, List<String>> next = new HashMap<>();
    for (String[] d : dependencies) next.computeIfAbsent(d[0], k -> new ArrayList<>()).add(d[1]);
    Set<String> seen = new HashSet<>(delayed);
    Deque<String> queue = new ArrayDeque<>(delayed);
    Set<String> affected = new TreeSet<>();
    while (!queue.isEmpty()) {
        for (String flight : next.getOrDefault(queue.poll(), List.of())) {
            if (seen.add(flight)) {
                affected.add(flight);
                queue.add(flight);
            }
        }
    }
    return affected;
}
