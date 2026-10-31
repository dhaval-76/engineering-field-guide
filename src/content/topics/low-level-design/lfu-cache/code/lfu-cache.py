from collections import OrderedDict, defaultdict
from typing import Any, Hashable

class LFUCache:
    """O(1) LFU: evict the least frequently used key; ties broken by least recently used."""

    def __init__(self, capacity: int):
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        self.capacity = capacity
        self._values: dict[Hashable, Any] = {}
        self._freq: dict[Hashable, int] = {}
        self._buckets: defaultdict[int, OrderedDict] = defaultdict(OrderedDict)  # freq -> keys, oldest first
        self._min_freq = 0

    def get(self, key: Hashable) -> Any | None:
        if key not in self._values:
            return None
        self._touch(key)
        return self._values[key]

    def put(self, key: Hashable, value: Any) -> None:
        if key in self._values:
            self._values[key] = value
            self._touch(key)
            return
        if len(self._values) == self.capacity:
            self._evict()
        self._values[key] = value
        self._freq[key] = 1
        self._buckets[1][key] = None
        self._min_freq = 1

    def _touch(self, key: Hashable) -> None:
        f = self._freq[key]
        del self._buckets[f][key]
        if not self._buckets[f]:
            del self._buckets[f]
            if self._min_freq == f:
                self._min_freq = f + 1
        self._freq[key] = f + 1
        self._buckets[f + 1][key] = None

    def _evict(self) -> None:
        bucket = self._buckets[self._min_freq]
        victim, _ = bucket.popitem(last=False)          # oldest key in the lowest-frequency bucket
        if not bucket:
            del self._buckets[self._min_freq]
        del self._values[victim]
        del self._freq[victim]

    def __len__(self) -> int:
        return len(self._values)
