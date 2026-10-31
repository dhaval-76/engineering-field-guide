import threading

class LeaseService:
    """Hands out a strictly increasing fencing token with every lease."""

    def __init__(self):
        self._next_token = 1
        self._holder: str | None = None
        self._expires_at = 0
        self._lock = threading.Lock()

    def acquire(self, client: str, now: int, ttl: int) -> int | None:
        with self._lock:
            if self._holder is not None and now < self._expires_at and self._holder != client:
                return None
            self._holder, self._expires_at = client, now + ttl
            token, self._next_token = self._next_token, self._next_token + 1
            return token

class FencedStorage:
    """Rejects writes carrying an older token than one already seen."""

    def __init__(self):
        self._highest: dict[str, int] = {}
        self._data: dict[str, str] = {}
        self._lock = threading.Lock()

    def write(self, key: str, value: str, token: int) -> bool:
        with self._lock:
            if token < self._highest.get(key, 0):
                return False                      # stale holder woke up late
            self._highest[key] = token
            self._data[key] = value
            return True

    def read(self, key: str) -> str | None:
        return self._data.get(key)
