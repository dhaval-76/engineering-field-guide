import threading
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum

# Static catalogue: these rarely change.
@dataclass(frozen=True)
class Movie:
    id: str
    title: str
    runtime: timedelta
    language: str

@dataclass(frozen=True)
class Seat:
    id: str
    row: str
    number: int
    category: str              # SILVER | GOLD | RECLINER

@dataclass(frozen=True)
class Show:                    # a movie on a screen at a time
    id: str
    movie_id: str
    screen_id: str
    starts_at: datetime
    prices: dict               # category -> paise

class ShowSeatStatus(Enum):
    AVAILABLE = "available"
    HELD = "held"
    BOOKED = "booked"

class ShowSeat:
    """Availability per show: the same seat is free at 6 pm and booked at 9 pm."""

    def __init__(self, show_id: str, seat_id: str):
        self.show_id, self.seat_id = show_id, seat_id
        self.status = ShowSeatStatus.AVAILABLE
        self._hold_id: str | None = None
        self._expires: datetime | None = None
        self._lock = threading.Lock()

    def try_hold(self, hold_id: str, now: datetime, ttl: timedelta) -> bool:
        with self._lock:
            if self.status is ShowSeatStatus.HELD and self._expires <= now:
                self.status = ShowSeatStatus.AVAILABLE            # lazy expiry
            if self.status is not ShowSeatStatus.AVAILABLE:
                return False
            self.status, self._hold_id, self._expires = ShowSeatStatus.HELD, hold_id, now + ttl
            return True

    def confirm(self, hold_id: str, now: datetime) -> bool:
        with self._lock:
            if self.status is not ShowSeatStatus.HELD or self._hold_id != hold_id or self._expires <= now:
                return False
            self.status = ShowSeatStatus.BOOKED
            return True

    def release(self, hold_id: str) -> None:
        with self._lock:
            if self.status is ShowSeatStatus.HELD and self._hold_id == hold_id:
                self.status, self._hold_id = ShowSeatStatus.AVAILABLE, None
