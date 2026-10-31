/**
 * One cache node, Redis-style: lazy + sampled active expiry, and approximate LRU eviction by sampling.
 * Keys also live in an array so a random key can be picked in O(1).
 */
class CacheNode {
    private static final class Item { String value; long expiresAt; long lastAccess; int slot; }

    private final int capacity, samples;
    private final Map<String, Item> items = new HashMap<>();
    private final List<String> keys = new ArrayList<>();          // for O(1) random sampling
    private final Random random;
    private long evictions = 0;

    CacheNode(int capacity, int samples, long seed) {
        this.capacity = capacity; this.samples = samples; this.random = new Random(seed);
    }

    synchronized String get(String key, long now) {
        Item it = items.get(key);
        if (it == null) return null;
        if (it.expiresAt <= now) { remove(key); return null; }   // lazy expiry on access
        it.lastAccess = now;
        return it.value;
    }

    synchronized void set(String key, String value, long ttlMillis, long now) {
        Item it = items.get(key);
        if (it == null) {
            if (items.size() >= capacity) evictOne(now);
            it = new Item();
            it.slot = keys.size();
            keys.add(key);
            items.put(key, it);
        }
        it.value = value;
        it.expiresAt = ttlMillis > 0 ? now + ttlMillis : Long.MAX_VALUE;
        it.lastAccess = now;
    }

    /** Approximate LRU: sample a few keys, evict the least recently used among them (expired first). */
    private void evictOne(long now) {
        String victim = null;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < samples; i++) {
            String k = keys.get(random.nextInt(keys.size()));
            Item it = items.get(k);
            if (it.expiresAt <= now) { victim = k; break; }
            if (it.lastAccess < oldest) { oldest = it.lastAccess; victim = k; }
        }
        remove(victim);
        evictions++;
    }

    /** Active expiry, run ~10×/s: sample keys; if more than 25% were expired, go again. */
    synchronized int activeExpireCycle(long now, int sampleSize) {
        int removed = 0, expiredInRound;
        do {
            expiredInRound = 0;
            int n = Math.min(sampleSize, keys.size());
            for (int i = 0; i < n; i++) {
                String k = keys.get(random.nextInt(keys.size()));
                if (items.get(k).expiresAt <= now) { remove(k); expiredInRound++; }
            }
            removed += expiredInRound;
        } while (!keys.isEmpty() && expiredInRound > sampleSize / 4);
        return removed;
    }

    private void remove(String key) {                             // swap-remove keeps the array dense
        Item it = items.remove(key);
        String last = keys.remove(keys.size() - 1);
        if (!last.equals(key)) { keys.set(it.slot, last); items.get(last).slot = it.slot; }
    }

    synchronized int size() { return items.size(); }
    synchronized long evictions() { return evictions; }
}
