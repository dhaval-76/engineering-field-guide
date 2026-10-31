import heapq
import itertools
import random
import threading
import time
from concurrent.futures import ThreadPoolExecutor


class TaskScheduler:
    """A min-heap of (run_at, seq, task) watched by one dispatcher thread; tasks run on a worker pool."""

    def __init__(self, workers: int = 4, clock=time.monotonic):
        self._heap: list = []
        self._seq = itertools.count()
        self._cancelled: set[int] = set()
        self._cv = threading.Condition()
        self._pool = ThreadPoolExecutor(max_workers=workers)
        self._clock = clock
        self._running = True
        threading.Thread(target=self._dispatch, daemon=True).start()

    def schedule(
        self,
        fn,
        delay_s: float,
        every_s: float | None = None,
        max_attempts: int = 3,
        base_s: float = 0.2,
    ) -> int:
        task_id = next(self._seq)
        self._push(self._clock() + delay_s, task_id, (fn, every_s, max_attempts, base_s, 1))
        return task_id

    def cancel(self, task_id: int) -> None:
        with self._cv:
            # skipped when it reaches the top of the heap
            self._cancelled.add(task_id)

    def _push(self, run_at: float, task_id: int, task) -> None:
        with self._cv:
            heapq.heappush(self._heap, (run_at, task_id, task))
            # wake the dispatcher: this may be the new earliest task
            self._cv.notify()

    def _dispatch(self) -> None:
        with self._cv:
            while self._running:
                if not self._heap:
                    self._cv.wait()
                    continue
                run_at, task_id, task = self._heap[0]
                wait = run_at - self._clock()
                if wait > 0:
                    self._cv.wait(timeout=wait)  # an earlier task may arrive meanwhile
                    continue
                heapq.heappop(self._heap)
                if task_id in self._cancelled:
                    self._cancelled.discard(task_id)
                    continue
                self._pool.submit(self._run, task_id, task)

    def _run(self, task_id: int, task) -> None:
        fn, every_s, max_attempts, base_s, attempt = task
        try:
            fn()
            if every_s is not None:
                self._push(self._clock() + every_s, task_id, (fn, every_s, max_attempts, base_s, 1))
        except Exception:
            if attempt < max_attempts:  # full jitter between retries
                self._push(
                    self._clock() + random.uniform(0, base_s * 2**attempt),
                    task_id,
                    (fn, every_s, max_attempts, base_s, attempt + 1),
                )
            # else: dead-letter / alert here

    def close(self) -> None:
        with self._cv:
            self._running = False
            self._cv.notify()
        self._pool.shutdown(wait=True)


# In-memory: a restart loses scheduled tasks. Durable schedules belong in a database or
# queue.
