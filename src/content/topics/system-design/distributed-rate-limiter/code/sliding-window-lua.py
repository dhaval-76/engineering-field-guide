import redis
from pathlib import Path

r = redis.Redis(socket_timeout=0.005)
# same Lua as the Java tab
sliding_window = r.register_script(Path("sliding_window.lua").read_text())

def check(key: str, limit: int, window_seconds: int) -> tuple[bool, int, int]:
    try:
        allowed, remaining, retry_after = sliding_window(keys=[f"rl:{key}"], args=[limit, window_seconds])
        return allowed == 1, remaining, retry_after
    except redis.RedisError:
        return True, -1, 0          # fail-open
