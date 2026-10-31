from dataclasses import dataclass, field

# A frozen dataclass gives an immutable value object; __post_init__ validates across fields.
# Keyword arguments with defaults cover optional fields, so Python rarely needs a builder class.
@dataclass(frozen=True)
class Pizza:
    size: str
    crust: str = "REGULAR"
    toppings: tuple[str, ...] = ()
    extra_cheese: bool = False

    def __post_init__(self):
        object.__setattr__(self, "toppings", tuple(self.toppings))   # defensive, immutable copy
        if self.crust == "STUFFED" and self.size != "LARGE":
            raise ValueError("stuffed crust is only available in LARGE")

p = Pizza("LARGE", crust="STUFFED", toppings=["olives"])
