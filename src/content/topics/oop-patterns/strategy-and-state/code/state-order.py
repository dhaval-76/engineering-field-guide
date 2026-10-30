from enum import Enum


class Status(Enum):
    PLACED = "PLACED"
    PAID = "PAID"
    SHIPPED = "SHIPPED"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


TRANSITIONS = {
    Status.PLACED: {"pay": Status.PAID, "cancel": Status.CANCELLED},
    Status.PAID: {"ship": Status.SHIPPED, "cancel": Status.CANCELLED},
    Status.SHIPPED: {"deliver": Status.DELIVERED},
    Status.DELIVERED: {},
    Status.CANCELLED: {},
}


class Order:
    def __init__(self):
        self.status = Status.PLACED

    def apply(self, action: str) -> None:
        nxt = TRANSITIONS[self.status].get(action)
        if nxt is None:
            raise ValueError(f"cannot {action} when {self.status.value}")
        self.status = nxt
