import math
import threading
import time
from collections import deque

class FixedWindowLimiter:
    """One counter per (key, window). Cheap, but allows 2× the limit across a boundary."""

    def __init__(self, limit: int, window_s: float, clock=time.monotonic):
        self.limit, self.window_s, self.clock = limit, window_s, clock
        self._state: dict[str, list[int]] = {}           # key -> [window, count]
        self._lock = threading.Lock()

    def try_acquire(self, key: str) -> bool:
        with self._lock:
            window = math.floor(self.clock() / self.window_s)
            s = self._state.get(key)
            if s is None or s[0] != window:
                s = self._state[key] = [window, 0]
            if s[1] >= self.limit:
                return False
            s[1] += 1
            return True

class SlidingLogLimiter:
    """Exact, but stores one timestamp per allowed request."""

    def __init__(self, limit: int, window_s: float, clock=time.monotonic):
        self.limit, self.window_s, self.clock = limit, window_s, clock
        self._logs: dict[str, deque] = {}
        self._lock = threading.Lock()

    def try_acquire(self, key: str) -> bool:
        with self._lock:
            now = self.clock()
            log = self._logs.setdefault(key, deque())
            while log and now - log[0] >= self.window_s:
                log.popleft()
            if len(log) >= self.limit:
                return False
            log.append(now)
            return True

class SlidingWindowCounterLimiter:
    """Weight the previous window by how much of it still overlaps. O(1) memory, smooths boundary bursts."""

    def __init__(self, limit: int, window_s: float, clock=time.monotonic):
        self.limit, self.window_s, self.clock = limit, window_s, clock
        self._state: dict[str, list] = {}                # key -> [window, current, previous]
        self._lock = threading.Lock()

    def try_acquire(self, key: str) -> bool:
        with self._lock:
            now = self.clock()
            window = math.floor(now / self.window_s)
            elapsed = (now % self.window_s) / self.window_s
            s = self._state.setdefault(key, [window, 0, 0])
            if s[0] != window:                           # roll windows forward
                s[2] = s[1] if s[0] == window - 1 else 0
                s[1], s[0] = 0, window
            if s[2] * (1 - elapsed) + s[1] >= self.limit:
                return False
            s[1] += 1
            return True
