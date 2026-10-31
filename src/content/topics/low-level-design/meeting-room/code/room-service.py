# uses Room, SlotTaken and TimeSlot from above
class NoRoomAvailable(Exception):
    pass

class RoomFinder:
    def __init__(self, rooms: list["Room"]):
        self.rooms = list(rooms)

    def candidates(self, s: "TimeSlot", people: int, needs: set[str] = frozenset()) -> list["Room"]:
        """Smallest room that fits — keeps big rooms free for big meetings."""
        fits = [r for r in self.rooms if r.capacity >= people and needs <= r.amenities and r.is_free(s)]
        return sorted(fits, key=lambda r: r.capacity)

    def book_best(self, s, people, needs, organizer, attendees=frozenset()):
        """Another thread may book a candidate between the search and our attempt: try the next."""
        for r in self.candidates(s, people, needs):
            try:
                return r.book(s, organizer, attendees)
            except SlotTaken:
                continue
        raise NoRoomAvailable(s)
