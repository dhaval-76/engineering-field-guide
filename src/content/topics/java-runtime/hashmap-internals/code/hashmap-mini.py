class MiniHashMap:
    """Python's dict is open-addressing; this mirrors Java's chaining for comparison."""

    def __init__(self):
        self._buckets: list[list] = [[] for _ in range(8)]
        self._size = 0

    def _index(self, key) -> int:
        h = hash(key)
        return (h ^ (h >> 16)) & (len(self._buckets) - 1)

    def get(self, key, default=None):
        for k, v in self._buckets[self._index(key)]:
            if k == key:
                return v
        return default

    def put(self, key, value) -> None:
        bucket = self._buckets[self._index(key)]
        for i, (k, _) in enumerate(bucket):
            if k == key:
                bucket[i] = (key, value)
                return
        bucket.append((key, value))
        self._size += 1
        if self._size > len(self._buckets) * 0.75:
            entries = [e for b in self._buckets for e in b]
            self._buckets = [[] for _ in range(len(self._buckets) * 2)]
            for k, v in entries:
                self._buckets[self._index(k)].append((k, v))
