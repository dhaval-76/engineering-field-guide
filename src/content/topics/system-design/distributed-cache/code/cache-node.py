import random
import threading

class CacheNode:
    """One cache node, Redis-style: lazy + sampled active expiry, approximate LRU by sampling."""

    def __init__(self, capacity: int, samples: int = 5, seed: int | None = None):
        self.capacity, self.samples = capacity, samples
        # key -> [value, expires_at, last_access, slot]
        self._items: dict[str, list] = {}
        self._keys: list[str] = []              # dense list for O(1) random picks
        self._rnd = random.Random(seed)
        self._lock = threading.Lock()
        self.evictions = 0

    def get(self, key: str, now: float):
        with self._lock:
            it = self._items.get(key)
            if it is None:
                return None
            if it[1] <= now:
                self._remove(key)               # lazy expiry
                return None
            it[2] = now
            return it[0]

    def set(self, key: str, value, ttl: float, now: float) -> None:
        with self._lock:
            it = self._items.get(key)
            if it is None:
                if len(self._items) >= self.capacity:
                    self._evict_one(now)
                it = [None, 0, 0, len(self._keys)]
                self._keys.append(key)
                self._items[key] = it
            it[0], it[1], it[2] = value, (now + ttl if ttl > 0 else float("inf")), now

    def _evict_one(self, now: float) -> None:
        victim, oldest = None, float("inf")
        for _ in range(self.samples):
            k = self._rnd.choice(self._keys)
            it = self._items[k]
            if it[1] <= now:
                victim = k
                break
            if it[2] < oldest:
                victim, oldest = k, it[2]
        self._remove(victim)
        self.evictions += 1

    def active_expire_cycle(self, now: float, sample_size: int = 20) -> int:
        """Run ~10×/s: sample keys; if more than 25% were expired, go again."""
        with self._lock:
            removed = 0
            while True:
                expired = 0
                for _ in range(min(sample_size, len(self._keys))):
                    k = self._rnd.choice(self._keys)
                    if self._items[k][1] <= now:
                        self._remove(k)
                        expired += 1
                removed += expired
                if not self._keys or expired <= sample_size / 4:
                    return removed

    def _remove(self, key: str) -> None:        # swap-remove keeps the list dense
        slot = self._items.pop(key)[3]
        last = self._keys.pop()
        if last != key:
            self._keys[slot] = last
            self._items[last][3] = slot

    def __len__(self) -> int:
        return len(self._items)
