# The same token bucket shared by many app servers: state in Redis, check-and-update in
# one Lua script.
import time
import redis

TOKEN_BUCKET_LUA = """
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
    
"""

r = redis.Redis()
# EVALSHA with automatic fallback to EVAL
token_bucket = r.register_script(TOKEN_BUCKET_LUA)


def try_acquire(client_id: str, capacity: int = 100, per_second: float = 50) -> bool:
    # Lua runs atomically on the Redis server — no race between app servers
    return (
        token_bucket(
            keys=[f"rl:{client_id}"], args=[capacity, per_second / 1000, int(time.time() * 1000)]
        )
        == 1
    )
