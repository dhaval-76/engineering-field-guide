int removeStones(int[][] stones) {
    Map<Integer, Integer> parent = new HashMap<>();
    for (int[] s : stones) join(parent, s[0], ~s[1]); // ~ keeps rows and columns apart
    Set<Integer> groups = new HashSet<>();
    for (int key : parent.keySet()) groups.add(root(parent, key));
    return stones.length - groups.size(); // each group can shrink to one stone
}

int root(Map<Integer, Integer> parent, int x) {
    parent.putIfAbsent(x, x);
    if (parent.get(x) != x) parent.put(x, root(parent, parent.get(x)));
    return parent.get(x);
}

void join(Map<Integer, Integer> parent, int a, int b) {
    parent.put(root(parent, a), root(parent, b));
}
