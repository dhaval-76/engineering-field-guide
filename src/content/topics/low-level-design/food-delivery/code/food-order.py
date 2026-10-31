import math
import threading
from dataclasses import dataclass, field
from typing import Callable

RULES = {
    ("PLACED", "ACCEPTED"): {"RESTAURANT"},
    ("PLACED", "REJECTED"): {"RESTAURANT", "SYSTEM"},
    ("PLACED", "CANCELLED"): {"CUSTOMER"},
    ("ACCEPTED", "PREPARING"): {"RESTAURANT"},
    ("ACCEPTED", "CANCELLED"): {"CUSTOMER", "SYSTEM"},
    ("PREPARING", "READY"): {"RESTAURANT"},
    ("READY", "PICKED_UP"): {"PARTNER"},
    ("PICKED_UP", "DELIVERED"): {"PARTNER"},
}

@dataclass(frozen=True)
class Location:
    lat: float
    lng: float

    def km(self, o: "Location") -> float:          # haversine distance
        d_lat, d_lng = math.radians(o.lat - self.lat), math.radians(o.lng - self.lng)
        a = math.sin(d_lat / 2) ** 2 + math.cos(math.radians(self.lat)) * math.cos(math.radians(o.lat)) * math.sin(d_lng / 2) ** 2
        return 6371 * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

@dataclass
class Order:
    id: str
    restaurant_id: str
    pickup: Location
    drop: Location
    total_paise: int
    listeners: list[Callable] = field(default_factory=list)
    status: str = "PLACED"
    partner_id: str | None = None
    history: list[str] = field(default_factory=lambda: ["PLACED"])
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def move_to(self, nxt: str, actor: str) -> None:
        with self._lock:
            if actor not in RULES.get((self.status, nxt), set()):
                raise ValueError(f"{actor} cannot move {self.id} from {self.status} to {nxt}")
            if nxt == "PICKED_UP" and self.partner_id is None:
                raise ValueError("no partner assigned")
            prev, self.status = self.status, nxt
            self.history.append(nxt)
        for listener in self.listeners:
            listener(self, prev, nxt)
