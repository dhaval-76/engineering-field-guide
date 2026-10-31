import math
import threading

CELL_DEG = 0.009          # ≈ 1 km of latitude

def distance_km(a, b) -> float:
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    h = math.sin((p2 - p1) / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(math.radians(b[1] - a[1]) / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(h))

def _cell(p) -> tuple[int, int]:
    return math.floor(p[0] / CELL_DEG), math.floor(p[1] / CELL_DEG)

class GridIndex:
    """Uniform ~1 km cells: updating a location is O(1); "nearby" scans the surrounding cells."""

    def __init__(self):
        self._cells: dict[tuple[int, int], set] = {}
        self._cell_of: dict[str, tuple[int, int]] = {}
        self._lock = threading.Lock()

    def update(self, driver, p: tuple[float, float]) -> None:   # called on every GPS ping
        key = _cell(p)
        with self._lock:
            old = self._cell_of.get(driver.id)
            if old is not None and old != key:
                self._cells.get(old, set()).discard(driver)
            self._cells.setdefault(key, set()).add(driver)
            self._cell_of[driver.id] = key
            driver.location = p

    def near(self, p: tuple[float, float], radius_km: float) -> list:
        r, (row, col) = math.ceil(radius_km), _cell(p)
        with self._lock:
            found = [d for dr in range(-r, r + 1) for dc in range(-r, r + 1)
                     for d in self._cells.get((row + dr, col + dc), ())]
        return [d for d in found if distance_km(d.location, p) <= radius_km]
