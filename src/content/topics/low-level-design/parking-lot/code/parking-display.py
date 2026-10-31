import threading
from collections import Counter

class DisplayBoard:
    """Keeps per-floor, per-size free counts; updated by spot events, never by scanning."""

    def __init__(self, spots):
        self._free = Counter((s.floor, s.size) for s in spots if s.is_free)
        self._lock = threading.Lock()

    def spot_changed(self, spot, now_free: bool) -> None:
        with self._lock:
            self._free[(spot.floor, spot.size)] += 1 if now_free else -1
        self.render(spot.floor)

    def free_on(self, floor: int, size) -> int:
        return self._free[(floor, size)]

    def render(self, floor: int) -> None:
        """Push to the physical board / websocket."""
