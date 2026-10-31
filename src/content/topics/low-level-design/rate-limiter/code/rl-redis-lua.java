// The same token bucket shared by many app servers: state in Redis, check-and-update in
// one Lua script.
private static final String TOKEN_BUCKET_LUA = """
    local key      = KEYS[1]
    local capacity = tonumber(ARGV[1])
    local rate     = tonumber(ARGV[2])        -- tokens per millisecond
    local now      = tonumber(ARGV[3])        -- milliseconds
    local s = redis.call('HMGET', key, 'tokens', 'ts')
    local tokens = tonumber(s[1]) or capacity
    local ts     = tonumber(s[2]) or now
    tokens = math.min(capacity, tokens + (now - ts) * rate)
    local allowed = 0
    if tokens >= 1 then tokens = tokens - 1; allowed = 1 end
    redis.call('HSET', key, 'tokens', tokens, 'ts', now)
    redis.call('PEXPIRE', key, math.ceil(capacity / rate) * 2)
    return allowed
    """;

boolean tryAcquire(String clientId) {
    Long r = redis.execute(new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class),
            List.of("rl:" + clientId), "100", String.valueOf(50 / 1000.0), String.valueOf(clock.millis()));
    // Lua runs atomically on the Redis server — no race between servers
    return r != null && r == 1L;
}
