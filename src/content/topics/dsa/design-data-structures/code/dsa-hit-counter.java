/** Hits in the last 300 seconds; timestamps in seconds, non-decreasing. */
class HitCounter {
    private static final int WINDOW = 300;
    // which second each bucket currently holds
    private final int[] times = new int[WINDOW];
    private final int[] counts = new int[WINDOW];

    void hit(int timestamp) {
        int b = timestamp % WINDOW;
        // bucket reused: reset
        if (times[b] != timestamp) { times[b] = timestamp; counts[b] = 0; }
        counts[b]++;
    }

    int getHits(int timestamp) {
        int total = 0;
        for (int b = 0; b < WINDOW; b++)
            if (timestamp - times[b] < WINDOW) total += counts[b];
        return total;
    }
}
// hit O(1), getHits O(300) = O(1) for a fixed window; memory fixed no matter how many
// hits.
