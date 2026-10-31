/** Lock service that hands out a strictly increasing fencing token with every lease. */
class LeaseService {
    private long nextToken = 1;
    private String holder;
    private long expiresAt;

    synchronized OptionalLong acquire(String client, long now, long ttl) {
        if (holder != null && now < expiresAt && !holder.equals(client)) return OptionalLong.empty();
        holder = client;
        expiresAt = now + ttl;
        return OptionalLong.of(nextToken++);
    }
}

/**
 * The protected resource checks tokens: a write with an older token than one already seen
 * is rejected.
 */
class FencedStorage {
    private final Map<String, Long> highestToken = new HashMap<>();
    private final Map<String, String> data = new HashMap<>();

    synchronized boolean write(String key, String value, long token) {
        long seen = highestToken.getOrDefault(key, 0L);
        // a stale lock holder woke up late
        if (token < seen) return false;
        highestToken.put(key, token);
        data.put(key, value);
        return true;
    }

    synchronized String read(String key) { return data.get(key); }
}
