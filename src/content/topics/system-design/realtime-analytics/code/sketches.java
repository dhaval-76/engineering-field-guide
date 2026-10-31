// Unique visitors per hour with Redis HyperLogLog: ~12 KB per key whatever the
// cardinality.
void track(String userId, Instant at) {
    String key = "uv:" + at.truncatedTo(ChronoUnit.HOURS);
    redis.opsForHyperLogLog().add(key, userId);                         // PFADD
    redis.expire(key, Duration.ofDays(2));
}
long uniqueToday(List<String> hourKeys) {
    // PFCOUNT merges hours
    return redis.opsForHyperLogLog().size(hourKeys.toArray(String[]::new));
}

/**
 * Approximate top-K: Count-Min Sketch estimates counts; a small heap keeps the current
 * leaders.
 */
final class TopK {
    private final int depth = 4, width = 1 << 16;
    private final long[][] table = new long[depth][width];
    private final int k;
    private final Map<String, Long> leaders = new HashMap<>();

    TopK(int k) { this.k = k; }

    void add(String item) {
        long estimate = Long.MAX_VALUE;
        for (int row = 0; row < depth; row++) {
            int col = Math.floorMod(Objects.hash(row, item), width);
            // min over rows limits over-counting
            estimate = Math.min(estimate, ++table[row][col]);
        }
        leaders.put(item, estimate);
        // drop the smallest leader
        if (leaders.size() > k) {
            leaders.entrySet().stream().min(Map.Entry.comparingByValue())
                .ifPresent(min -> leaders.remove(min.getKey()));
        }
    }

    List<Map.Entry<String, Long>> top() {
        return leaders.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).toList();
    }
}
// Per partition: keep a TopK each; every few seconds merge the partitions' candidates for
// the global top-K.
