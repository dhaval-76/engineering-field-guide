List<List<Integer>> kSmallestPairs(int[] a, int[] b, int k) {
    // {sum, i, j}: for each a[i], walk along b
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
    for (int i = 0; i < Math.min(a.length, k); i++) heap.add(new int[] {a[i] + b[0], i, 0});
    List<List<Integer>> out = new ArrayList<>();
    while (out.size() < k && !heap.isEmpty()) {
        int[] top = heap.poll();
        out.add(List.of(a[top[1]], b[top[2]]));
        if (top[2] + 1 < b.length) {
            heap.add(new int[] {a[top[1]] + b[top[2] + 1], top[1], top[2] + 1});
        }
    }
    return out;
}
