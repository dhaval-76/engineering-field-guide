int[][] merge(int[][] intervals) {
    int[][] sorted = intervals.clone();
    Arrays.sort(sorted, Comparator.comparingInt(i -> i[0]));
    List<int[]> out = new ArrayList<>();
    for (int[] cur : sorted) {
        int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
        // overlap: extend
        if (last != null && cur[0] <= last[1]) last[1] = Math.max(last[1], cur[1]);
        // gap: new interval
        else out.add(new int[]{cur[0], cur[1]});
    }
    return out.toArray(new int[0][]);
}
