import bisect
import threading
import uuid
from dataclasses import dataclass
from datetime import datetime

@dataclass(frozen=True)
class TimeSlot:
    """Half-open [start, end): a meeting ending at 10:00 doesn't clash with one starting at 10:00."""
    start: datetime
    end: datetime

    def __post_init__(self):
        if not self.start < self.end:
            raise ValueError("start must be before end")

    def overlaps(self, o: "TimeSlot") -> bool:
        return self.start < o.end and o.start < self.end

@dataclass(frozen=True)
class Booking:
    id: str
    room_id: str
    slot: TimeSlot
    organizer: str
    attendees: frozenset
    series_id: str | None

class SlotTaken(Exception):
    pass

class Room:
    def __init__(self, id: str, capacity: int, amenities: set[str]):
        self.id, self.capacity, self.amenities = id, capacity, frozenset(amenities)
        self._starts: list[datetime] = []          # sorted; bookings never overlap
        self._bookings: list[Booking] = []         # parallel to _starts
        self.lock = threading.RLock()              # re-entrant: series booking holds it across calls

    def is_free(self, s: TimeSlot) -> bool:
        """O(log n): only the booking just before and the one just after can overlap."""
        with self.lock:
            i = bisect.bisect_left(self._starts, s.start)
            before = self._bookings[i - 1] if i > 0 else None
            after = self._bookings[i] if i < len(self._bookings) else None
            return not (before and before.slot.overlaps(s)) and not (after and after.slot.overlaps(s))

    def book(self, s: TimeSlot, organizer: str, attendees=frozenset(), series_id=None) -> Booking:
        with self.lock:                            # check and insert under one lock
            if not self.is_free(s):
                raise SlotTaken(self.id, s)
            b = Booking(str(uuid.uuid4()), self.id, s, organizer, frozenset(attendees), series_id)
            i = bisect.bisect_left(self._starts, s.start)
            self._starts.insert(i, s.start)
            self._bookings.insert(i, b)
            return b

    def cancel(self, b: Booking) -> None:
        with self.lock:
            i = bisect.bisect_left(self._starts, b.slot.start)
            if i < len(self._bookings) and self._bookings[i] == b:
                del self._starts[i], self._bookings[i]
