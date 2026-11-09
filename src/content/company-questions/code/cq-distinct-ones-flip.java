int distinctOneCounts(int[] arr) {
    // Flipping a subarray changes the count by (zeros - ones) in it, so a 0 scores +1
    // and a 1 scores -1. Every total between the smallest and largest sum is reachable.
    int maxEnding = 0, minEnding = 0, best = 0, worst = 0;
    for (int bit : arr) {
        int gain = bit == 0 ? 1 : -1;
        maxEnding = Math.max(0, maxEnding + gain);
        minEnding = Math.min(0, minEnding + gain);
        best = Math.max(best, maxEnding);
        worst = Math.min(worst, minEnding);
    }
    return best - worst + 1;
}
