@Component
class SlidingWindowLimiter {
    // KEYS[1] = key prefix; ARGV[1] = limit; ARGV[2] = window in seconds.
    // Uses Redis's own clock, so every gateway agrees on window boundaries.
    private static final RedisScript<List> SCRIPT = RedisScript.of("""
        local t = redis.call('TIME')
        local now = tonumber(t[1]) + tonumber(t[2]) / 1e6
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[1])
        local current = math.floor(now / window)
        local curKey = KEYS[1] .. ':' .. current
        local prevKey = KEYS[1] .. ':' .. (current - 1)
        local prev = tonumber(redis.call('GET', prevKey) or '0')
        local cur = tonumber(redis.call('GET', curKey) or '0')
        local elapsed = (now % window) / window
        local estimate = prev * (1 - elapsed) + cur
        if estimate >= limit then
          local retry = math.ceil(window * (1 - elapsed))
          return {0, 0, retry}
        end
        redis.call('INCR', curKey)
        redis.call('EXPIRE', curKey, window * 2)        -- set in the same atomic script
        return {1, math.floor(limit - estimate - 1), 0}
        """, List.class);

    private final StringRedisTemplate redis;

    Decision check(String key, int limit, Duration window) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> r = redis.execute(SCRIPT, List.of("rl:" + key),
                    String.valueOf(limit), String.valueOf(window.toSeconds()));
            return new Decision(r.get(0) == 1L, r.get(1), r.get(2));
        } catch (DataAccessException redisDown) {
            return Decision.allowDegraded();           // fail-open; a local limiter can back this up
        }
    }

    record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
        static Decision allowDegraded() { return new Decision(true, -1, 0); }
    }
}
// Configure the Redis client with a short command timeout (e.g. 5 ms) so a slow Redis can't slow every request.
