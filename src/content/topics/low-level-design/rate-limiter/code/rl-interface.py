import time
from typing import Callable, Protocol

class RateLimiter(Protocol):
    def try_acquire(self, key: str) -> bool:
        """True if the request identified by `key` (user, API key, IP) may proceed now."""
        ...

Clock = Callable[[], float]          # seconds; inject a fake in tests so they don't sleep
SYSTEM_CLOCK: Clock = time.monotonic
