# uses Level from above. The standard library offers the same idea: logging.handlers.QueueHandler + QueueListener.
import queue
import threading

class AsyncAppender:
    """Hands events to a background thread so request threads never wait on disk or network."""

    def __init__(self, target, capacity: int = 10_000):
        self.target = target
        self._queue: queue.Queue = queue.Queue(maxsize=capacity)
        self.dropped = 0
        self._closed = False
        self._worker = threading.Thread(target=self._drain, name="log-writer", daemon=True)
        self._worker.start()

    def append(self, e) -> None:
        if self._closed:
            return
        if e.level >= Level.WARN:
            self._queue.put(e)                     # never drop warnings/errors: wait for room
            return
        try:
            self._queue.put_nowait(e)
        except queue.Full:
            self.dropped += 1                      # full: drop DEBUG/INFO and count it

    def _drain(self) -> None:
        while not (self._closed and self._queue.empty()):
            try:
                self.target.append(self._queue.get(timeout=0.1))
            except queue.Empty:
                pass

    def close(self) -> None:                       # flush on shutdown
        self._closed = True
        self._worker.join(timeout=5)
