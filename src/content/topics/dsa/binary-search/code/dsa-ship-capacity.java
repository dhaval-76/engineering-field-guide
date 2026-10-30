int shipWithinDays(int[] weights, int days) {
    int lo = 0, hi = 0;
    for (int w : weights) { lo = Math.max(lo, w); hi += w; }   // must fit the heaviest; all in one day works
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (canShip(weights, days, mid)) hi = mid; else lo = mid + 1;
    }
    return lo;
}

/** Greedy: fill each day up to capacity; monotone in capacity. O(n). */
boolean canShip(int[] weights, int days, int capacity) {
    int used = 1, load = 0;
    for (int w : weights) {
        if (load + w > capacity) { used++; load = 0; }
        load += w;
    }
    return used <= days;
}
// [1..10], days 5 → 15. O(n log(sum)) overall.
