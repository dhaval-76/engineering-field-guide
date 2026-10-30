/** Minimum number of intervals to remove so the rest don't overlap (touching ends are fine). */
int eraseOverlapIntervals(int[][] intervals) {
    int[][] sorted = intervals.clone();
    Arrays.sort(sorted, Comparator.comparingInt(i -> i[1]));     // earliest END first
    int kept = 0;
    long lastEnd = Long.MIN_VALUE;
    for (int[] iv : sorted) {
        if (iv[0] >= lastEnd) { kept++; lastEnd = iv[1]; }        // compatible: keep it
    }
    return intervals.length - kept;
}
// [[1,2],[2,3],[3,4],[1,3]] → 1 (remove [1,3])
