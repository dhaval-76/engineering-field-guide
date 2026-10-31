// Before trusting a greedy rule, compare it with an exact answer on many small inputs.
// Example: "largest coin first" for coin change. Returns the first amount where greedy
// uses more coins than the optimum (from DP), or -1 if none up to the limit.
int firstGreedyFailure(int[] coins, int limit) {
    int[] sorted = coins.clone();
    Arrays.sort(sorted);
    int[] best = new int[limit + 1];             // DP: fewest coins for each amount
    Arrays.fill(best, Integer.MAX_VALUE);
    best[0] = 0;
    for (int a = 1; a <= limit; a++) {
        for (int c : sorted) {
            if (c <= a && best[a - c] != Integer.MAX_VALUE) {
                best[a] = Math.min(best[a], best[a - c] + 1);
            }
        }
    }
    for (int a = 1; a <= limit; a++) {
        int left = a, used = 0;
        for (int i = sorted.length - 1; i >= 0; i--) {   // greedy: largest coin first
            used += left / sorted[i];
            left %= sorted[i];
        }
        // greedy works, but uses more coins
        if (left == 0 && used > best[a]) return a;
    }
    return -1;
}
// {1,3,4} → 6 (greedy 4+1+1, optimal 3+3). {1,2,5,10,20,50,100,200,500,2000} → -1:
// real currencies are designed so greedy works ("canonical" coin systems).
