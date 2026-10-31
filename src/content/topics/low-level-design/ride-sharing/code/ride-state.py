from dataclasses import dataclass, field, replace
from datetime import datetime, timezone

NEXT = {
    "REQUESTED": {"DRIVER_ASSIGNED", "CANCELLED"},
    # back to REQUESTED if the driver drops
    "DRIVER_ASSIGNED": {"DRIVER_ARRIVED", "REQUESTED", "CANCELLED"},
    "DRIVER_ARRIVED": {"IN_PROGRESS", "CANCELLED"},
    "IN_PROGRESS": {"COMPLETED"},
    "COMPLETED": set(),
    "CANCELLED": set(),
}

@dataclass(frozen=True)
class Ride:
    id: str
    status: str = "REQUESTED"
    history: tuple = field(default_factory=tuple)

def transition(ride: Ride, nxt: str, actor: str, at: datetime | None = None) -> Ride:
    """Returns a new Ride; illegal moves raise instead of silently corrupting state."""
    if nxt not in NEXT[ride.status]:
        raise ValueError(f"{ride.status} → {nxt} not allowed")
    entry = (ride.status, nxt, actor, at or datetime.now(timezone.utc))
    return replace(ride, status=nxt, history=ride.history + (entry,))
