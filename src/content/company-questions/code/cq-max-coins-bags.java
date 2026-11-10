/** coins[i] = {left, right, c}: every bag from left to right holds c coins; ranges don't overlap. */
long maximumCoins(int[][] coins, int k) {
    int[][] sorted = coins.clone();
    Arrays.sort(sorted, Comparator.comparingInt(s -> s[0]));
    // A best window can always be moved until it starts at a range's left end or ends at a
    // range's right end. Check the first case, then mirror the line to check the second.
    int[][] mirrored = new int[sorted.length][];
    for (int i = 0; i < sorted.length; i++) {
        int[] s = sorted[sorted.length - 1 - i];
        mirrored[i] = new int[] {-s[1], -s[0], s[2]};
    }
    return Math.max(windowsStartingAtLeftEnds(sorted, k), windowsStartingAtLeftEnds(mirrored, k));
}

long windowsStartingAtLeftEnds(int[][] ranges, int k) {
    long full = 0, best = 0; // full = coins of ranges entirely inside the window
    int j = 0;
    for (int i = 0; i < ranges.length; i++) {
        long end = ranges[i][0] + (long) k - 1;
        while (j < ranges.length && ranges[j][1] <= end) {
            full += (long) (ranges[j][1] - ranges[j][0] + 1) * ranges[j][2];
            j++;
        }
        long partial = j < ranges.length && ranges[j][0] <= end
                ? (end - ranges[j][0] + 1) * ranges[j][2] : 0;
        best = Math.max(best, full + partial);
        if (j > i) full -= (long) (ranges[i][1] - ranges[i][0] + 1) * ranges[i][2];
        else j = i + 1; // range i was longer than k: it was only ever partial
    }
    return best;
}
