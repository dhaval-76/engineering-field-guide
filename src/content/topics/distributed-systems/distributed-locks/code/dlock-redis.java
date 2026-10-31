// Jedis. A lease-based lock: unique token, expiry, and owner-checked release/extend.
class RedisLock {
    private static final String RELEASE = """
        if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end""";
    private static final String EXTEND = """
        if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end""";

    private final JedisPool pool;
    RedisLock(JedisPool pool) { this.pool = pool; }

    /** Returns the token if acquired, empty otherwise. */
    Optional<String> tryAcquire(String key, Duration ttl) {
        // identifies this holder
        String token = UUID.randomUUID().toString();
        try (Jedis j = pool.getResource()) {
            String ok = j.set(key, token, SetParams.setParams().nx().px(ttl.toMillis()));
            return "OK".equals(ok) ? Optional.of(token) : Optional.empty();
        }
    }

    /**
     * Only the holder can release: never DEL a lock that expired and was taken by someone
     * else.
     */
    boolean release(String key, String token) {
        try (Jedis j = pool.getResource()) {
            return Long.valueOf(1).equals(j.eval(RELEASE, List.of(key), List.of(token)));
        }
    }

    // heartbeat for long work
    boolean extend(String key, String token, Duration ttl) {
        try (Jedis j = pool.getResource()) {
            return Long.valueOf(1).equals(j.eval(EXTEND, List.of(key), List.of(token, String.valueOf(ttl.toMillis()))));
        }
    }
}

// Usage: an efficiency lock — at worst two workers do the job twice.
redisLock.tryAcquire("lock:report:2026-10-30", Duration.ofMinutes(5)).ifPresent(token -> {
    try { buildDailyReport(); }
    finally { redisLock.release("lock:report:2026-10-30", token); }
});
