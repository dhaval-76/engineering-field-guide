from bisect import bisect_left, insort


class ElevatorCar:
    def __init__(self, car_id: int, min_floor: int, max_floor: int, floor: int = 0):
        self.id, self.min_floor, self.max_floor, self.floor = car_id, min_floor, max_floor, floor
        self.direction = "IDLE"
        self._stops: list[int] = []  # kept sorted
        self._door_open = False

    def add_stop(self, f: int) -> None:
        if not self.min_floor <= f <= self.max_floor:
            raise ValueError(f"no floor {f}")
        if f not in self._stops:
            insort(self._stops, f)
        if self.direction == "IDLE" and f != self.floor:
            self.direction = "UP" if f > self.floor else "DOWN"

    @property
    def pending_stops(self) -> int:
        return len(self._stops)

    def tick(self) -> None:
        if self._door_open:
            self._door_open = False
        elif self.floor in self._stops:
            self._stops.remove(self.floor)
            self._door_open = True
        elif (nxt := self._next_stop()) is None:
            self.direction = "IDLE"
        else:
            self.direction = "UP" if nxt > self.floor else "DOWN"
            self.floor += 1 if self.direction == "UP" else -1

    def _next_stop(self):
        if not self._stops:
            return None
        i = bisect_left(self._stops, self.floor)
        above = self._stops[i] if i < len(self._stops) else None
        below = self._stops[i - 1] if i > 0 else None
        if self.direction == "DOWN":
            return below if below is not None else above
        return above if above is not None else below
