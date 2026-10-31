from dataclasses import dataclass

# Location (with .km) is defined with the Order model above.

@dataclass(frozen=True)
class Partner:
    id: str
    location: Location
    available: bool
    rating: float
    active_orders: int

def nearest_available(max_km: float):
    """Strategy: nearest free partner within range; ties go to the higher rating."""
    def choose(order, partners):
        fits = [p for p in partners
                if p.available and p.active_orders == 0 and p.location.km(order.pickup) <= max_km]
        return min(fits, key=lambda p: (p.location.km(order.pickup), -p.rating), default=None)
    return choose

class Dispatcher:
    """Listener: assign when the restaurant accepts; free the partner when the order ends."""

    def __init__(self, strategy, partners_nearby):
        self.strategy, self.partners_nearby, self.busy = strategy, partners_nearby, set()

    def __call__(self, order, prev, nxt):
        if nxt == "ACCEPTED":
            p = self.strategy(order, [x for x in self.partners_nearby() if x.id not in self.busy])
            if p:
                self.busy.add(p.id)
                order.partner_id = p.id
        if nxt in ("DELIVERED", "CANCELLED") and order.partner_id:
            self.busy.discard(order.partner_id)

def delivery_fee_paise(order, surge: float, discount_paise: int) -> int:
    """₹20 for the first 2 km, ₹8/km after, times surge, minus discount, never negative."""
    base = 2000 + round(max(0.0, order.pickup.km(order.drop) - 2) * 800)
    return max(0, round(base * surge) - discount_paise)
