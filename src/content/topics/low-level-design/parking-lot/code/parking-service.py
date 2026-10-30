import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone

FITS = {"MOTORCYCLE": ["SMALL", "MEDIUM", "LARGE"], "CAR": ["MEDIUM", "LARGE"], "TRUCK": ["LARGE"]}


@dataclass
class Spot:
    id: str
    floor: int
    size: str
    occupant: object = None


@dataclass
class Ticket:
    vehicle: object
    spot: Spot
    entry: datetime
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    status: str = "ACTIVE"


class ParkingLot:
    def __init__(self, spots, pricing, now=lambda: datetime.now(timezone.utc)):
        self._spots, self._pricing, self._now = spots, pricing, now
        self._floor_locks: dict[int, threading.Lock] = {s.floor: threading.Lock() for s in spots}
        self._active: dict[str, Ticket] = {}

    def park(self, vehicle) -> Ticket | None:
        fits = FITS[vehicle.type]
        for floor in sorted(self._floor_locks):
            with self._floor_locks[floor]:  # find-and-claim is atomic per floor
                free = [s for s in self._spots if s.floor == floor and s.occupant is None and s.size in fits]
                if free:
                    spot = min(free, key=lambda s: fits.index(s.size))
                    spot.occupant = vehicle
                    ticket = Ticket(vehicle, spot, self._now())
                    self._active[ticket.id] = ticket
                    return ticket
        return None

    def exit(self, ticket_id: str) -> None:
        ticket = self._active.pop(ticket_id)
        if ticket.status != "PAID":
            self._active[ticket_id] = ticket
            raise ValueError("pay before exit")
        with self._floor_locks[ticket.spot.floor]:
            ticket.spot.occupant = None
        ticket.status = "CLOSED"
