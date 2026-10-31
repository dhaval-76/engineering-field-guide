import logging
import threading
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from typing import Callable

log = logging.getLogger(__name__)

class EventBus:
    """In-process event bus: handlers per event class, run on a pool and isolated from each other."""

    def __init__(self, executor: ThreadPoolExecutor | None = None):
        self._handlers: dict[type, list[Callable]] = defaultdict(list)
        self._lock = threading.Lock()
        self._executor = executor or ThreadPoolExecutor(max_workers=4)

    def subscribe(self, event_type: type, handler: Callable) -> Callable[[], None]:
        with self._lock:
            self._handlers[event_type].append(handler)
        def unsubscribe():
            with self._lock:
                self._handlers[event_type].remove(handler)
        return unsubscribe

    def publish(self, event) -> None:
        with self._lock:
            # snapshot: safe if handlers change
            handlers = list(self._handlers.get(type(event), ()))
        for h in handlers:
            self._executor.submit(self._safe, h, event)

    @staticmethod
    def _safe(handler, event) -> None:
        try:
            handler(event)
        except Exception:
            # one bad handler doesn't stop the others
            log.exception("handler failed for %r", event)

    def close(self) -> None:
        self._executor.shutdown(wait=True)

# bus = EventBus(); unsubscribe = bus.subscribe(OrderPlaced, email.send)
# bus.publish(OrderPlaced("o-1", "c-9", 1000))
