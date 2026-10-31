from bisect import bisect_left


# Bookings arrive one at a time as half-open [start, end). Reject one that overlaps any
# booking. Keep bookings sorted by start; only the neighbours on either side of the
# insertion point can clash. O(log n) search + O(n) insert (sortedcontainers.SortedList
# makes it O(log n)).
class MyCalendar:
    def __init__(self) -> None:
        self.starts: list[int] = []
        self.ends: list[int] = []

    def book(self, start: int, end: int) -> bool:
        i = bisect_left(self.starts, start)
        # the one before is still running
        if i > 0 and self.ends[i - 1] > start:
            return False
        if i < len(self.starts) and self.starts[i] < end:   # the next one starts too soon
            return False
        self.starts.insert(i, start)
        self.ends.insert(i, end)
        return True
# book(10,20) True, book(15,25) False, book(20,30) True (touching is fine).
