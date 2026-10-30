import threading

class Inventory:
    def __init__(self):
        self._stock: dict[str, int] = {}
        self._lock = threading.Lock()          # RLock if the same thread may re-enter

    def reserve(self, sku: str, qty: int) -> bool:
        with self._lock:                        # acquire/release, even on exceptions
            have = self._stock.get(sku, 0)
            if have < qty:
                return False
            self._stock[sku] = have - qty
            return True
