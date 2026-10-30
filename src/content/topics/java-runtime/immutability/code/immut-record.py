from dataclasses import dataclass, replace


@dataclass(frozen=True)
class Money:
    minor_units: int
    currency: str

    def __post_init__(self):
        if self.minor_units < 0:
            raise ValueError("negative amount")

    def plus(self, other: "Money") -> "Money":
        if self.currency != other.currency:
            raise ValueError("currency mismatch")
        return replace(self, minor_units=self.minor_units + other.minor_units)


@dataclass(frozen=True)
class Basket:
    id: str
    skus: tuple[str, ...]   # use tuples/frozensets for immutable contents
