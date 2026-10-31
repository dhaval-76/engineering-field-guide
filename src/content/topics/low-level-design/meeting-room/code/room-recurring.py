import uuid
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from zoneinfo import ZoneInfo

@dataclass(frozen=True)
class Recurrence:
    first_day: date
    until: date
    weekdays: frozenset          # 0 = Monday … 6 = Sunday
    start: time
    length: timedelta
    zone: str

    def expand(self) -> list["TimeSlot"]:
        """Concrete slots. Local time + zone, so 10:00 stays 10:00 across DST changes."""
        out, d = [], self.first_day
        while d <= self.until:
            if d.weekday() in self.weekdays:
                s = datetime.combine(d, self.start, tzinfo=ZoneInfo(self.zone))
                out.append(TimeSlot(s, s + self.length))
            d += timedelta(days=1)
        return out

class SeriesConflict(Exception):
    def __init__(self, clashes):
        super().__init__(f"{len(clashes)} occurrences clash")
        self.clashes = clashes

def book_series(room: "Room", rule: Recurrence, organizer: str, attendees=frozenset()) -> list["Booking"]:
    """All-or-nothing: hold the room's lock, check every occurrence, then insert all."""
    series_id, slots = str(uuid.uuid4()), rule.expand()
    with room.lock:
        clashes = [s for s in slots if not room.is_free(s)]
        if clashes:
            raise SeriesConflict(clashes)          # let the user skip those dates or pick another room
        return [room.book(s, organizer, attendees, series_id) for s in slots]
