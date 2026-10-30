/** A single node's in-memory store with TTL. Values and expiries live together, so overwrites can't leave stale TTLs. */
final class KvNode {
    private record Entry(byte[] value, long expiresAtMs) {      // 0 = no expiry
        boolean expired(long now) { return expiresAtMs != 0 && now >= expiresAtMs; }
    }

    private final ConcurrentHashMap<String, Entry> data = new ConcurrentHashMap<>();
    private final Clock clock;

    KvNode(Clock clock) { this.clock = clock; }

    Optional<byte[]> get(String key) {
        long now = clock.millis();
        Entry e = data.get(key);
        if (e == null) return Optional.empty();
        if (e.expired(now)) { data.remove(key, e); return Optional.empty(); }   // lazy expiry; only this exact entry
        return Optional.of(e.value());
    }

    void put(String key, byte[] value, Duration ttl) {
        long expiresAt = ttl == null ? 0 : clock.millis() + ttl.toMillis();
        data.put(key, new Entry(value, expiresAt));          // replaces value AND expiry atomically
    }

    void delete(String key) { data.remove(key); }

    /** Active expiry: sample a bounded number of keys per pass (Redis does a similar sampling). */
    int sweep(int maxChecks) {
        long now = clock.millis();
        int removed = 0, checked = 0;
        for (var it = data.entrySet().iterator(); it.hasNext() && checked++ < maxChecks; ) {
            var e = it.next();
            if (e.getValue().expired(now) && data.remove(e.getKey(), e.getValue())) removed++;
        }
        return removed;
    }
}
