/** O(1) LFU: evict the least frequently used key; ties broken by least recently used. */
class LFUCache<K, V> {
    private record Entry<V>(V value, int freq) {}

    private final int capacity;
    private final Map<K, Entry<V>> entries = new HashMap<>();
    // freq → keys, oldest first
    private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
    private int minFreq = 0;

    LFUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    V get(K key) {
        Entry<V> e = entries.get(key);
        if (e == null) return null;
        touch(key, e);
        return e.value();
    }

    void put(K key, V value) {
        Entry<V> e = entries.get(key);
        if (e != null) {                                   // update counts as a use
            entries.put(key, new Entry<>(value, e.freq()));
            touch(key, entries.get(key));
            return;
        }
        if (entries.size() == capacity) evict();
        entries.put(key, new Entry<>(value, 1));
        buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        // a new key always has the lowest count
        minFreq = 1;
    }

    // move key from bucket f to f + 1
    private void touch(K key, Entry<V> e) {
        int f = e.freq();
        LinkedHashSet<K> bucket = buckets.get(f);
        bucket.remove(key);
        if (bucket.isEmpty()) {
            buckets.remove(f);
            if (minFreq == f) minFreq = f + 1;
        }
        entries.put(key, new Entry<>(e.value(), f + 1));
        buckets.computeIfAbsent(f + 1, x -> new LinkedHashSet<>()).add(key);
    }

    private void evict() {
        LinkedHashSet<K> bucket = buckets.get(minFreq);
        // least recently used among the least frequent
        K victim = bucket.iterator().next();
        bucket.remove(victim);
        if (bucket.isEmpty()) buckets.remove(minFreq);
        entries.remove(victim);
    }

    int size() { return entries.size(); }
}
