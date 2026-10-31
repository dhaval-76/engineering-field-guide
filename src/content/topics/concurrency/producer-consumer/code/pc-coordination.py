import threading
from concurrent.futures import ThreadPoolExecutor, wait

# Semaphore: at most 10 concurrent calls to a fragile downstream
permits = threading.BoundedSemaphore(10)

def call_partner(req):
    if not permits.acquire(timeout=0.1):
        raise TooBusy()
    try:
        return partner_api.call(req)
    finally:
        permits.release()

# CountDownLatch: wait until N one-time events have happened (e.g. caches warmed)
with ThreadPoolExecutor() as pool:
    done, not_done = wait([pool.submit(c.warm) for c in caches], timeout=30)
    if not_done:
        log.warning("starting with cold caches")

# CyclicBarrier: N threads meet at a point repeatedly (simulations, parallel phases)
barrier = threading.Barrier(4, action=merge_partial_results)
