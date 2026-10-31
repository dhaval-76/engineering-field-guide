import queue
import threading
import time
from typing import Callable

class LeakyBucket:
    """Requests wait in a bounded queue and are released at a constant rate — perfectly smooth output."""

    def __init__(self, capacity: int, per_second: float, downstream: Callable):
        self._queue: queue.Queue = queue.Queue(maxsize=capacity)
        self._period = 1 / per_second
        self._downstream = downstream
        self._stop = threading.Event()
        threading.Thread(target=self._drain, daemon=True).start()

    def offer(self, request) -> bool:
        """False = bucket full; reject (HTTP 429) or shed load."""
        try:
            self._queue.put_nowait(request)
            return True
        except queue.Full:
            return False

    def _drain(self) -> None:
        next_tick = time.monotonic()
        while not self._stop.is_set():
            try:
                self._downstream(self._queue.get_nowait())
            except queue.Empty:
                pass
            next_tick += self._period
            time.sleep(max(0.0, next_tick - time.monotonic()))     # fixed rate, no drift

    def close(self) -> None:
        self._stop.set()
