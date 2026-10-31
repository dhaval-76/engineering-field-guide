import threading
import uuid
from dataclasses import dataclass
from typing import Callable

@dataclass
class _Level:
    on_hand: int = 0
    reserved: int = 0

    @property
    def available(self) -> int:
        return self.on_hand - self.reserved

@dataclass(frozen=True)
class Reservation:
    id: str
    order_id: str
    warehouse: str
    sku: str
    qty: int
    expires_at: int

def most_stock_first(sku: str, qty: int, available: dict[str, int]) -> str | None:
    """Strategy: a warehouse that can fill the whole quantity; most stock first, then alphabetical."""
    fits = [(-n, wh) for wh, n in available.items() if n >= qty]
    return min(fits)[1] if fits else None

class InventoryService:
    def __init__(self, strategy: Callable[[str, int, dict[str, int]], str | None] = most_stock_first):
        self.strategy = strategy
        self._levels: dict[tuple[str, str], _Level] = {}
        self._reservations: dict[str, Reservation] = {}
        self._reorder_point: dict[str, int] = {}
        self._listeners: list[Callable[[str, int], None]] = []
        self.movements: list[str] = []
        self._lock = threading.Lock()

    def receive(self, warehouse: str, sku: str, qty: int) -> None:
        with self._lock:
            self._levels.setdefault((warehouse, sku), _Level()).on_hand += qty
            self.movements.append(f"RECEIVE {warehouse} {sku} +{qty}")

    def reserve(self, order_id: str, sku: str, qty: int, now: int, ttl: int) -> Reservation | None:
        with self._lock:
            self._expire(now)
            available = {wh: lvl.available for (wh, s), lvl in self._levels.items() if s == sku}
            warehouse = self.strategy(sku, qty, available)
            if warehouse is None:
                return None                                   # never oversell
            self._levels[(warehouse, sku)].reserved += qty
            r = Reservation(str(uuid.uuid4()), order_id, warehouse, sku, qty, now + ttl)
            self._reservations[r.id] = r
            self.movements.append(f"RESERVE {warehouse} {sku} {qty} for {order_id}")
            self._check_reorder(sku)
            return r

    def confirm(self, reservation_id: str, now: int) -> bool:
        with self._lock:
            self._expire(now)
            r = self._reservations.pop(reservation_id, None)
            if r is None:
                return False
            lvl = self._levels[(r.warehouse, r.sku)]
            lvl.reserved -= r.qty
            lvl.on_hand -= r.qty
            self.movements.append(f"SHIP {r.warehouse} {r.sku} -{r.qty} for {r.order_id}")
            return True

    def cancel(self, reservation_id: str) -> None:
        with self._lock:
            r = self._reservations.pop(reservation_id, None)
            if r:
                self._release(r, "CANCEL")

    def available(self, sku: str) -> int:
        return sum(lvl.available for (_, s), lvl in self._levels.items() if s == sku)

    def set_reorder_point(self, sku: str, point: int) -> None:
        self._reorder_point[sku] = point

    def on_low_stock(self, listener: Callable[[str, int], None]) -> None:
        self._listeners.append(listener)

    def _expire(self, now: int) -> None:
        for r in [r for r in self._reservations.values() if r.expires_at <= now]:
            del self._reservations[r.id]
            self._release(r, "EXPIRE")

    def _release(self, r: Reservation, why: str) -> None:
        self._levels[(r.warehouse, r.sku)].reserved -= r.qty
        self.movements.append(f"{why} {r.warehouse} {r.sku} {r.qty} for {r.order_id}")

    def _check_reorder(self, sku: str) -> None:
        point, total = self._reorder_point.get(sku), self.available(sku)
        if point is not None and total <= point:
            for listener in self._listeners:
                listener(sku, total)
