# uses ElevatorCar from above
from dataclasses import dataclass

@dataclass(frozen=True)
class HallCall:
    floor: int
    wants: str            # "UP" | "DOWN"

def nearest_suitable_car(top_floor: int):
    """Cost = floors to travel before reaching the caller, given each car's current direction."""
    def cost(car, call: HallCall) -> int:
        here, target = car.floor, call.floor
        if car.direction == "IDLE":
            return abs(here - target)
        if car.direction == "UP":
            return target - here if target >= here and call.wants == "UP" else (top_floor - here) + abs(top_floor - target)
        # sweep to 0, then up
        return here - target if target <= here and call.wants == "DOWN" else here + target

    def choose(call: HallCall, cars: list):
        return min(cars, key=lambda c: (cost(c, call), c.pending_stops))
    return choose

class ElevatorController:
    def __init__(self, cars: list, strategy):
        self.cars, self.strategy = cars, strategy

    def hall_call(self, floor: int, wants: str) -> None:
        self.strategy(HallCall(floor, wants), self.cars).add_stop(floor)

    def car_call(self, car_id: int, floor: int) -> None:
        self.cars[car_id].add_stop(floor)

    def tick(self) -> None:
        for c in self.cars:
            c.tick()
