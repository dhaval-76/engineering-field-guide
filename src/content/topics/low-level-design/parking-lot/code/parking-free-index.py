# uses FITS_IN, SpotSize and ParkingSpot from the model above
import heapq
import threading

class FreeSpotIndex:
    """O(log n) allocation: one min-heap of free spots per size, ordered by (floor, id) = "nearest"."""

    def __init__(self, spots: list["ParkingSpot"]):
        self._free = {size: [] for size in SpotSize}
        for s in spots:
            heapq.heappush(self._free[s.size], (s.floor, s.id, s))
        # pop + push must not interleave across threads
        self._lock = threading.Lock()

    def claim(self, vehicle_type) -> "ParkingSpot | None":
        with self._lock:
            for size in FITS_IN[vehicle_type]:
                if self._free[size]:
                    return heapq.heappop(self._free[size])[2]
            return None

    def give_back(self, s: "ParkingSpot") -> None:
        with self._lock:
            heapq.heappush(self._free[s.size], (s.floor, s.id, s))
