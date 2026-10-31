import threading
from cachetools import TTLCache, cached

class CachingCatalog:
    """Proxy: same interface as the real catalog (by_id), controls ACCESS to it — here, caching."""

    def __init__(self, real):
        self._real = real
        cache = TTLCache(maxsize=10_000, ttl=5 * 60)
        # `condition` makes concurrent misses for one id wait for a single load
        self.by_id = cached(cache, condition=threading.Condition())(real.by_id)

# Other proxy flavours: access control (check permissions before delegating), lazy loading,
# remote proxies (an HTTP/gRPC client stub), and __getattr__-based proxies that forward any call.
