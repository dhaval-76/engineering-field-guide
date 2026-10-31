import threading
from dataclasses import dataclass
from datetime import datetime
from enum import Enum

class VehicleType(Enum):
    MOTORCYCLE = "motorcycle"
    CAR = "car"
    TRUCK = "truck"

class SpotSize(Enum):
    SMALL = 1
    MEDIUM = 2
    LARGE = 3

@dataclass(frozen=True)
class Vehicle:
    plate: str
    type: VehicleType

# Which spot sizes a vehicle may use, smallest first (so cars don't take truck spots unnecessarily).
FITS_IN = {
    VehicleType.MOTORCYCLE: [SpotSize.SMALL, SpotSize.MEDIUM, SpotSize.LARGE],
    VehicleType.CAR: [SpotSize.MEDIUM, SpotSize.LARGE],
    VehicleType.TRUCK: [SpotSize.LARGE],
}

class ParkingSpot:
    def __init__(self, id: str, floor: int, size: SpotSize):
        self.id, self.floor, self.size = id, floor, size
        self._occupant: Vehicle | None = None
        self._lock = threading.Lock()

    def try_occupy(self, v: Vehicle) -> bool:
        """Atomic claim: exactly one caller can move this spot from free to occupied."""
        with self._lock:
            if self._occupant is not None:
                return False
            self._occupant = v
            return True

    def release(self) -> None:
        with self._lock:
            self._occupant = None

    @property
    def is_free(self) -> bool:
        return self._occupant is None

class Ticket:
    def __init__(self, id: str, vehicle: Vehicle, spot: ParkingSpot, entry: datetime):
        self.id, self.vehicle, self.spot, self.entry = id, vehicle, spot, entry
        self.status, self.fee = "ACTIVE", None
        self._lock = threading.Lock()

    def mark_paid(self, fee: int) -> None:
        with self._lock:
            if self.status != "ACTIVE":
                raise ValueError(f"ticket {self.id} is {self.status}")
            self.fee, self.status = fee, "PAID"

    def close(self) -> None:
        with self._lock:
            if self.status != "PAID":
                raise ValueError("pay before exit")
            self.status = "CLOSED"
