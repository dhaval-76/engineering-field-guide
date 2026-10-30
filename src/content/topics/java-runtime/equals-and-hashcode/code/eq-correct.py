from dataclasses import dataclass


# frozen=True generates __eq__ and __hash__ from the fields, and makes instances immutable.
@dataclass(frozen=True)
class Money:
    amount_minor: int
    currency: str


assert Money(100, "INR") == Money(100, "INR")
assert len({Money(100, "INR"), Money(100, "INR")}) == 1

# If you define __eq__ yourself on a mutable class, Python sets __hash__ = None,
# making instances unhashable — a deliberate guard against the mutable-key bug.
