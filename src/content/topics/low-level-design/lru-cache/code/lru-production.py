# pip install cachetools — size-bounded, TTL-aware caches for Python.
import threading
from cachetools import TTLCache, cached

# `condition` makes concurrent callers for the same missing key wait for one load instead of all
# hitting the database (a plain `lock=` protects the cache but lets every caller load: 20 loads vs 1).
@cached(cache=TTLCache(maxsize=10_000, ttl=30 * 60), condition=threading.Condition())
def load_profile(user_id: str) -> dict:
    return user_repository.load(user_id)

profile = load_profile(user_id)

# Single-process, no TTL needed? The standard library has it: @functools.lru_cache(maxsize=10_000)
