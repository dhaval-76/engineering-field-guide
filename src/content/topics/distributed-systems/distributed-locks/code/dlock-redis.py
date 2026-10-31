# redis-py. A lease-based lock: unique token, expiry, owner-checked release/extend.
import uuid
import redis

RELEASE = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
EXTEND = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end"

r = redis.Redis()


def try_acquire(key: str, ttl_ms: int) -> str | None:
    token = str(uuid.uuid4())
    return token if r.set(key, token, nx=True, px=ttl_ms) else None


def release(key: str, token: str) -> bool:
    return r.eval(RELEASE, 1, key, token) == 1


def extend(key: str, token: str, ttl_ms: int) -> bool:
    return r.eval(EXTEND, 1, key, token, ttl_ms) == 1


# Usage: an efficiency lock — at worst two workers do the job twice.
if token := try_acquire("lock:report:2026-10-30", 5 * 60_000):
    try:
        build_daily_report()
    finally:
        release("lock:report:2026-10-30", token)
