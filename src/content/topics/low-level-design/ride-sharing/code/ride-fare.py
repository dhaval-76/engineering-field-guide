from dataclasses import dataclass

@dataclass(frozen=True)
class Rates:              # paise
    base: int
    per_km: int
    per_minute: int
    minimum_fare: int

@dataclass(frozen=True)
class TripEstimate:
    vehicle: str
    km: float
    minutes: float

def standard_fare(rates: dict[str, Rates]):
    """Fare policy as a strategy function."""
    def estimate(t: TripEstimate, surge: float) -> int:
        r = rates[t.vehicle]
        raw = r.base + r.per_km * t.km + r.per_minute * t.minutes
        return max(round(raw * surge), r.minimum_fare)
    return estimate

def surge_multiplier(open_requests: int, available_drivers: int) -> float:
    """Demand / supply over the last few minutes, smoothed and capped."""
    if available_drivers == 0:
        return 2.5
    return min(2.5, max(1.0, 0.8 + 0.4 * open_requests / available_drivers))
