import threading
import time


class TokenBucketLimiter:
    def __init__(self, capacity: int, per_second: float, clock=time.monotonic):
        self.capacity, self.rate, self.clock = capacity, per_second, clock
        self._buckets: dict[str, list[float]] = {}  # key -> [tokens, last]
        self._lock = threading.Lock()

    def try_acquire(self, key: str) -> bool:
        with self._lock:
            now = self.clock()
            tokens, last = self._buckets.get(key, [self.capacity, now])
            tokens = min(self.capacity, tokens + (now - last) * self.rate)
            allowed = tokens >= 1
            self._buckets[key] = [tokens - 1 if allowed else tokens, now]
            return allowed
