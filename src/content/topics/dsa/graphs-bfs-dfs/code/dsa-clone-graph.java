class Node {
    int val;
    List<Node> neighbors = new ArrayList<>();
    Node(int val) { this.val = val; }
}

Node cloneGraph(Node start) {
    if (start == null) return null;
    Map<Node, Node> copies = new HashMap<>();          // original → copy; doubles as "visited"
    Deque<Node> queue = new ArrayDeque<>(List.of(start));
    copies.put(start, new Node(start.val));
    while (!queue.isEmpty()) {
        Node n = queue.poll();
        for (Node nb : n.neighbors) {
            if (!copies.containsKey(nb)) {
                copies.put(nb, new Node(nb.val));
                queue.offer(nb);
            }
            copies.get(n).neighbors.add(copies.get(nb));   // wire copy → copy
        }
    }
    return copies.get(start);
}
