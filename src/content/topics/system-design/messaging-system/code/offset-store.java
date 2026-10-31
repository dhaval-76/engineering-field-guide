/**
 * Committed offsets per (group, topic, partition). Real systems store these as records in
 * a compacted log.
 */
final class OffsetStore {
    private record Key(String group, String topic, int partition) {}
    private final ConcurrentHashMap<Key, Long> committed = new ConcurrentHashMap<>();

    /** Commit the NEXT offset to read — i.e. last processed + 1. */
    void commit(String group, String topic, int partition, long lastProcessed) {
        // never move backwards by accident
        committed.merge(new Key(group, topic, partition), lastProcessed + 1, Math::max);
    }

    /**
     * Where a consumer starts: committed position, or the reset policy for a new group.
     */
    long startOffset(String group, String topic, int partition, long logStart, long logEnd, boolean earliest) {
        Long c = committed.get(new Key(group, topic, partition));
        // new group, or data expired
        if (c == null || c < logStart) return earliest ? logStart : logEnd;
        return c;
    }
}

/**
 * Range assignment for one topic: partitions split as evenly as possible across sorted
 * members.
 */
static Map<String, List<Integer>> assign(List<String> members, int partitions) {
    List<String> sorted = members.stream().sorted().toList();
    Map<String, List<Integer>> out = new LinkedHashMap<>();
    sorted.forEach(m -> out.put(m, new ArrayList<>()));
    for (int p = 0; p < partitions; p++) out.get(sorted.get(p % sorted.size())).add(p);
    return out;    // members beyond the partition count get nothing
}
