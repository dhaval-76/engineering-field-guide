int[][] insert(int[][] intervals, int[] add) {
    List<int[]> out = new ArrayList<>();
    int i = 0, n = intervals.length;
    // 1. entirely before
    while (i < n && intervals[i][1] < add[0]) out.add(intervals[i++]);
    int s = add[0], e = add[1];
    // 2. overlapping: absorb
    while (i < n && intervals[i][0] <= e) {
        s = Math.min(s, intervals[i][0]);
        e = Math.max(e, intervals[i][1]);
        i++;
    }
    out.add(new int[]{s, e});
    // 3. entirely after
    while (i < n) out.add(intervals[i++]);
    return out.toArray(new int[0][]);
}
// [[1,3],[6,9]] + [2,5] → [[1,5],[6,9]]. O(n), no sorting needed.
