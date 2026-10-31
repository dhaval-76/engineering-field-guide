# uses GridIndex and distance_km from above
import threading

class Driver:
    def __init__(self, id: str, vehicle: str, rating: float):
        self.id, self.vehicle, self.rating = id, vehicle, rating
        self.location = None
        self.status = "OFFLINE"            # OFFLINE | AVAILABLE | OFFERED | ON_TRIP
        self._lock = threading.Lock()

    def try_reserve(self) -> bool:
        """Only one ride can reserve a driver: AVAILABLE → OFFERED is a compare-and-set."""
        with self._lock:
            if self.status != "AVAILABLE":
                return False
            self.status = "OFFERED"
            return True

    def release(self) -> None:
        with self._lock:
            self.status = "AVAILABLE"

def nearest_good_driver(ride, candidates):
    """Closer is better; rating breaks near-ties."""
    return sorted(candidates, key=lambda d: distance_km(d.location, ride.pickup) - 0.3 * d.rating)

class Dispatcher:
    def __init__(self, index, strategy, offers):
        # offers.offer_and_await(d, ride, s) -> bool
        self.index, self.strategy, self.offers = index, strategy, offers

    def match(self, ride, wanted: str):
        """Widen the radius until someone accepts or we give up."""
        for radius in (1, 2, 4, 8):
            candidates = [d for d in self.index.near(ride.pickup, radius)
                          if d.vehicle == wanted and d.status == "AVAILABLE"]
            for d in self.strategy(ride, candidates):
                if not d.try_reserve():
                    continue                         # another ride got this driver first
                if self.offers.offer_and_await(d, ride, 15):
                    ride.driver_id = d.id
                    return d
                d.release()                          # declined or timed out
        return None
