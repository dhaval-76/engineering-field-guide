import threading
from concurrent.futures import ThreadPoolExecutor

t = threading.Thread(target=lambda: print("hello"), name="worker-1")
t.start()
t.join()

# CPython's GIL lets only one thread run Python bytecode at a time: threads help I/O-bound
# work, processes (multiprocessing / ProcessPoolExecutor) give CPU parallelism. Races
# still happen — `counter += 1` is several bytecodes and threads can switch in between.
with ThreadPoolExecutor(max_workers=8) as pool:
    results = list(pool.map(fetch, urls))
