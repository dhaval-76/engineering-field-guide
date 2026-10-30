import queue, threading

orders: queue.Queue = queue.Queue(maxsize=1000)       # thread-safe, bounded

def worker():
    while True:
        order = orders.get()                          # blocks while empty
        if order is None:                             # sentinel → stop
            break
        try:
            fulfil(order)
        finally:
            orders.task_done()

threads = [threading.Thread(target=worker, daemon=True) for _ in range(4)]
for t in threads: t.start()

orders.put(order, timeout=0.05)                       # raises queue.Full → backpressure
# asyncio has the same shape: asyncio.Queue(maxsize=...) with await q.put()/q.get()
