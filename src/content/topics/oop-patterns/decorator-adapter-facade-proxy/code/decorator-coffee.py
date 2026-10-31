from dataclasses import dataclass

@dataclass(frozen=True)
class Espresso:
    description: str = "Espresso"
    price_paise: int = 12_000

# Add-ons wrap any beverage — combinations happen at runtime, not as subclasses.
@dataclass(frozen=True)
class WithMilk:
    inner: object
    @property
    def description(self) -> str: return f"{self.inner.description}, milk"
    @property
    def price_paise(self) -> int: return self.inner.price_paise + 2_000

@dataclass(frozen=True)
class WithSyrup:
    inner: object
    flavour: str
    @property
    def description(self) -> str: return f"{self.inner.description}, {self.flavour} syrup"
    @property
    def price_paise(self) -> int: return self.inner.price_paise + 3_000

order = WithSyrup(WithMilk(Espresso()), "hazelnut")
# "Espresso, milk, hazelnut syrup" — 17000 paise (₹170.00)
