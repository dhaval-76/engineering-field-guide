// 1. Single-flight inside one instance: concurrent misses for the same key share ONE
// load.
//    Caffeine's AsyncLoadingCache does this natively; this is the idea.
class SingleFlight<K, V> {
    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    V get(K key, Function<K, V> loader) {
        CompletableFuture<V> mine = new CompletableFuture<>();
        CompletableFuture<V> existing = inFlight.putIfAbsent(key, mine);
        // someone is already loading
        if (existing != null) return existing.join();
        try {
            V v = loader.apply(key);
            mine.complete(v);
            return v;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }
}

// 2. Across instances: a short Redis lock so only one instance rebuilds a hot key.
ProductView getWithLock(long id) {
    String key = "product:v1:" + id, lockKey = "lock:" + key;
    for (int attempt = 0; attempt < 50; attempt++) {
        String cached = redis.opsForValue().get(key);
        if (cached != null) return read(cached);

        String token = UUID.randomUUID().toString();             // identifies OUR lock
        // SET NX EX
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(acquired)) {
            try {
                ProductView v = load(id);
                redis.opsForValue().set(key, write(v), jitter(TTL));
                return v;
            } finally {
                // Release only if it's still ours (it may have expired and been taken by
                // another).
                redis.execute(RELEASE_IF_OWNER, List.of(lockKey), token);
            }
        }
        // someone else is loading: wait, re-check
        sleep(Duration.ofMillis(50));
    }
    // give up waiting; protect latency
    return load(id);
}

static final RedisScript<Long> RELEASE_IF_OWNER = RedisScript.of("""
    if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end
    """, Long.class);

// 3. Stale-while-revalidate: store the value with a "soft" expiry inside it and a longer
//    real TTL. After the soft expiry, return the stale value immediately and refresh it
//    in the background (once — guarded by the lock above). Users never wait on a rebuild.
record Cached<T>(T value, Instant softExpiry) {}
