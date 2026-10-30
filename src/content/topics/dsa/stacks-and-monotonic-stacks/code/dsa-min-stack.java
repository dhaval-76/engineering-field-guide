class MinStack {
    private final Deque<int[]> stack = new ArrayDeque<>();   // {value, min of this and everything below}

    void push(int x) {
        int min = stack.isEmpty() ? x : Math.min(x, stack.peek()[1]);
        stack.push(new int[]{x, min});
    }
    void pop()    { stack.pop(); }
    int top()     { return stack.peek()[0]; }
    int getMin()  { return stack.peek()[1]; }               // O(1): the min is stored with each entry
}
