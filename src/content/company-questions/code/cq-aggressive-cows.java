int largestMinDistance(int[] stalls, int cows) {
    Arrays.sort(stalls);
    int lo = 1, hi = stalls[stalls.length - 1] - stalls[0];
    while (lo < hi) { // largest distance that still fits every cow
        int mid = (lo + hi + 1) >>> 1;
        if (fits(stalls, cows, mid)) lo = mid;
        else hi = mid - 1;
    }
    return lo;
}

boolean fits(int[] stalls, int cows, int gap) {
    int placed = 1, last = stalls[0];
    for (int s : stalls) {
        if (s - last >= gap) {
            placed++;
            last = s;
        }
    }
    return placed >= cows;
}
