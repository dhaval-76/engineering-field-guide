import threading
from concurrent.futures import ThreadPoolExecutor

class UnsafeCounter:
    def __init__(self):
        self.count = 0

    def increment(self):
        self.count += 1          # read → add → write: the GIL does NOT make this one atomic step

counter = UnsafeCounter()
with ThreadPoolExecutor(max_workers=8) as pool:
    for _ in range(100_000):
        pool.submit(counter.increment)
print(counter.count)             # can be LESS than 100000 — and on free-threaded Python (3.13t) it often is

# Check-then-act is the same bug at a larger scale:
if key not in cache:             # two threads can both see "absent" and both load
    cache[key] = load(key)
# ✓ do the check and the update under one lock
cache_lock = threading.Lock()
with cache_lock:
    if key not in cache:
        cache[key] = load(key)
