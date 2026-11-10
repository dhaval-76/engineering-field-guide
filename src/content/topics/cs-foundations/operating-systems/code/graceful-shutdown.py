import signal
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor

pool = ThreadPoolExecutor(max_workers=4)
stop = threading.Event()


def on_sigterm(signum, frame):
    # Runs in the main thread. Python's default for SIGTERM is to die at once.
    print("SIGTERM: draining", flush=True)
    stop.set()


def job():
    time.sleep(0.5)  # a request in flight
    print("job done", flush=True)


signal.signal(signal.SIGTERM, on_sigterm)  # nothing can catch SIGKILL
for _ in range(3):
    pool.submit(job)
stop.wait()  # serve until a signal arrives
pool.shutdown(wait=True)  # accept no new work, finish what is running
print("drained", flush=True)
sys.exit(0)
