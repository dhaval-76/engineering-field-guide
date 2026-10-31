import threading
import time

class KvNode:
    """A single node's in-memory store with TTL. Value and expiry live together, so overwrites can't leave stale TTLs."""

    def __init__(self, clock=time.monotonic):
        self._data: dict[str, tuple[bytes, float]] = {}    # key -> (value, expires_at); 0 = no expiry
        self._clock = clock
        self._lock = threading.Lock()

    def get(self, key: str) -> bytes | None:
        with self._lock:
            e = self._data.get(key)
            if e is None:
                return None
            if e[1] and self._clock() >= e[1]:
                del self._data[key]                          # lazy expiry
                return None
            return e[0]

    def put(self, key: str, value: bytes, ttl: float | None = None) -> None:
        with self._lock:
            self._data[key] = (value, self._clock() + ttl if ttl else 0)   # value AND expiry replaced together

    def delete(self, key: str) -> None:
        with self._lock:
            self._data.pop(key, None)

    def sweep(self, max_checks: int) -> int:
        """Active expiry: check a bounded number of keys per pass (Redis samples similarly)."""
        with self._lock:
            now = self._clock()
            expired = [k for k, (_, exp) in list(self._data.items())[:max_checks] if exp and now >= exp]
            for k in expired:
                del self._data[k]
            return len(expired)
