import threading
from enum import Enum

class DoorState(Enum):
    CLOSED = "closed"
    OPENING = "opening"
    OPEN = "open"
    CLOSING = "closing"

# (state, event) → next state; anything not listed leaves the state unchanged.
TRANSITIONS = {
    (DoorState.CLOSED, "OPEN_REQUESTED"): DoorState.OPENING,
    (DoorState.OPENING, "OPENED"): DoorState.OPEN,
    (DoorState.OPEN, "CLOSE_REQUESTED"): DoorState.CLOSING,
    (DoorState.OPEN, "TIMEOUT"): DoorState.CLOSING,
    (DoorState.CLOSING, "OBSTRUCTION"): DoorState.OPENING,     # safety: always re-open
    (DoorState.CLOSING, "OPEN_REQUESTED"): DoorState.OPENING,
    (DoorState.CLOSING, "CLOSED_FULLY"): DoorState.CLOSED,
}

class Door:
    def __init__(self):
        self.state = DoorState.CLOSED
        self._lock = threading.Lock()

    def on(self, event: str) -> DoorState:
        with self._lock:
            self.state = TRANSITIONS.get((self.state, event), self.state)
            return self.state
