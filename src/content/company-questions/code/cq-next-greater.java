int[] nextGreaterElement(int[] queries, int[] nums) {
    Map<Integer, Integer> nextBigger = new HashMap<>();
    Deque<Integer> stack = new ArrayDeque<>(); // values still waiting for a bigger one
    for (int n : nums) {
        while (!stack.isEmpty() && stack.peek() < n) nextBigger.put(stack.pop(), n);
        stack.push(n);
    }
    int[] out = new int[queries.length];
    for (int i = 0; i < queries.length; i++) out[i] = nextBigger.getOrDefault(queries[i], -1);
    return out;
}
