int sumSubarrayMins(int[] arr) {
    long mod = 1_000_000_007L, total = 0;
    int n = arr.length;
    Deque<Integer> stack = new ArrayDeque<>(); // indexes with increasing values
    for (int i = 0; i <= n; i++) {
        int value = i == n ? Integer.MIN_VALUE : arr[i]; // sentinel flushes the stack
        while (!stack.isEmpty() && arr[stack.peek()] >= value) {
            int mid = stack.pop();
            int left = stack.isEmpty() ? -1 : stack.peek();
            // arr[mid] is the minimum of every subarray from (left, mid] to [mid, i)
            total = (total + (long) arr[mid] * (mid - left) * (i - mid)) % mod;
        }
        stack.push(i);
    }
    return (int) total;
}
