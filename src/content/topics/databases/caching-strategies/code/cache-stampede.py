import json
import random
import threading
import time
import uuid
from concurrent.futures import Future

# 1. Single-flight inside one process: concurrent misses for the same key share ONE load.
class SingleFlight:
    def __init__(self):
        self._in_flight: dict[str, Future] = {}
        self._lock = threading.Lock()

    def get(self, key: str, loader):
        with self._lock:
            fut = self._in_flight.get(key)
            mine = fut is None
            if mine:
                fut = self._in_flight[key] = Future()
        if not mine:
            return fut.result()                          # someone is already loading
        try:
            value = loader(key)
            fut.set_result(value)
            return value
        except Exception as e:
            fut.set_exception(e)
            raise
        finally:
            with self._lock:
                self._in_flight.pop(key, None)

# 2. Across instances (redis-py): a short Redis lock so only one instance rebuilds a hot key.
RELEASE_IF_OWNER = "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end"

def get_with_lock(r, product_id: int, load, ttl: int = 300):
    key, lock_key = f"product:v1:{product_id}", f"lock:product:v1:{product_id}"
    for _ in range(50):
        cached = r.get(key)
        if cached is not None:
            return json.loads(cached)
        token = str(uuid.uuid4())                        # identifies OUR lock
        if r.set(lock_key, token, nx=True, ex=5):
            try:
                value = load(product_id)
                r.set(key, json.dumps(value), ex=ttl + random.randint(0, ttl // 10))   # jitter spreads expiries
                return value
            finally:
                r.eval(RELEASE_IF_OWNER, 1, lock_key, token)   # release only if still ours
        time.sleep(0.05)                                 # someone else is loading: wait, re-check
    return load(product_id)                              # give up waiting; protect latency

# 3. Stale-while-revalidate: store {"value", "soft_expiry"} with a longer real TTL. After soft_expiry,
#    return the stale value at once and refresh in the background (guarded by the lock above).
