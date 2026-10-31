import itertools
import threading

# 1. Mutual exclusion — one thread in the critical section at a time
class LockedCounter:
    def __init__(self):
        self._count = 0
        self._lock = threading.Lock()

    def increment(self) -> None:
        with self._lock:
            self._count += 1

    def get(self) -> int:
        with self._lock:
            return self._count

# 2. itertools.count: next() is a single C-level call, atomic under the GIL (CPython detail —
#    not guaranteed on free-threaded builds; prefer the lock when in doubt)
ids = itertools.count(1)
next_id = next(ids)

# 3. Across processes: multiprocessing.Value(..., lock=True), or Redis INCR across servers.
