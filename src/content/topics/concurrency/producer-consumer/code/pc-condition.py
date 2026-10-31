import threading
from collections import deque

class BoundedQueue:
    """What queue.Queue does inside: one lock, two conditions, so producers and consumers wait separately."""

    def __init__(self, capacity: int):
        self._items: deque = deque()
        self._capacity = capacity
        lock = threading.Lock()
        self._not_full = threading.Condition(lock)
        self._not_empty = threading.Condition(lock)

    def put(self, item) -> None:
        with self._not_full:
            # loop: re-check after every wake-up
            while len(self._items) == self._capacity:
                self._not_full.wait()
            self._items.append(item)
            # wake one consumer — only consumers wait here
            self._not_empty.notify()

    def take(self):
        with self._not_empty:
            while not self._items:
                self._not_empty.wait()
            item = self._items.popleft()
            self._not_full.notify()                     # wake one producer
            return item
