import math
import threading

CELL = 0.01   # degrees ≈ 1.1 km of latitude; one city shard holds a few thousand cells

def km(lat1, lon1, lat2, lon2) -> float:
    p1, p2, dl = math.radians(lat1), math.radians(lat2), math.radians(lon2 - lon1)
    a = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(a))

def cell_of(lat: float, lon: float) -> tuple[int, int]:
    return math.floor(lat / CELL), math.floor(lon / CELL)

class DriverIndex:
    """In-memory index of moving drivers: frequent updates, stale entries expire, nearest-k by rings."""

    def __init__(self, stale_after_s: float = 30):
        self.stale_after_s = stale_after_s
        # id -> (lat, lon, seen_at, available)
        self._drivers: dict[str, tuple[float, float, float, bool]] = {}
        self._cells: dict[tuple[int, int], set[str]] = {}
        self._lock = threading.Lock()

    def update(self, driver_id: str, lat: float, lon: float, available: bool, now: float) -> None:
        with self._lock:
            old = self._drivers.get(driver_id)
            if old is not None:
                old_cell = cell_of(old[0], old[1])
                # moved across a cell boundary
                if old_cell != cell_of(lat, lon):
                    self._cells[old_cell].discard(driver_id)
            self._drivers[driver_id] = (lat, lon, now, available)
            self._cells.setdefault(cell_of(lat, lon), set()).add(driver_id)

    def claim(self, driver_id: str) -> bool:
        """Atomically take an available driver for one trip offer (two riders can't both get them)."""
        with self._lock:
            d = self._drivers.get(driver_id)
            if d is None or not d[3]:
                return False
            self._drivers[driver_id] = (d[0], d[1], d[2], False)
            return True

    def evict_stale(self, now: float) -> int:
        """Run every few seconds: drop drivers whose app stopped reporting."""
        with self._lock:
            gone = [d for d, v in self._drivers.items() if now - v[2] > self.stale_after_s]
            for d in gone:
                lat, lon, _, _ = self._drivers.pop(d)
                self._cells[cell_of(lat, lon)].discard(d)
            return len(gone)

    def nearest(self, lat: float, lon: float, k: int, max_km: float, now: float) -> list[tuple[float, str]]:
        """Search rings of cells outward until k drivers are found or the radius is exhausted."""
        cy, cx = cell_of(lat, lon)
        found: list[tuple[float, str]] = []
        max_ring = math.ceil(max_km / (CELL * 111 * max(math.cos(math.radians(lat)), 0.1))) + 1
        with self._lock:
            for ring in range(max_ring + 1):
                for dy in range(-ring, ring + 1):
                    for dx in range(-ring, ring + 1):
                        # only the new outer ring
                        if max(abs(dy), abs(dx)) != ring:
                            continue
                        for d in self._cells.get((cy + dy, cx + dx), ()):
                            dlat, dlon, seen, available = self._drivers[d]
                            if available and now - seen <= self.stale_after_s:
                                dist = km(lat, lon, dlat, dlon)
                                if dist <= max_km:
                                    found.append((dist, d))
                # every unvisited cell is at least ring * cell-size away; stop once k are
                # closer than that
                found.sort()
                ring_km = ring * CELL * 111 * max(math.cos(math.radians(lat)), 0.1)
                if len(found) >= k and found[k - 1][0] <= ring_km:
                    break
        return found[:k]
