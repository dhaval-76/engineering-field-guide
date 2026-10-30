// O(2ⁿ) time, O(n) stack: the same subproblems are recomputed again and again
long fibNaive(int n) { return n < 2 ? n : fibNaive(n - 1) + fibNaive(n - 2); }

// O(n) time, O(n) space: memoisation — each subproblem solved once
long fibMemo(int n, long[] memo) {
    if (n < 2) return n;
    if (memo[n] != 0) return memo[n];
    return memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
}

// O(n) time, O(1) space: keep only what the next step needs
long fib(int n) {
    long prev = 0, cur = 1;
    for (int i = 0; i < n; i++) { long next = prev + cur; prev = cur; cur = next; }
    return prev;
}
// fibNaive(45) makes ~3.7 billion calls; fib(45) makes 45 loop iterations.
