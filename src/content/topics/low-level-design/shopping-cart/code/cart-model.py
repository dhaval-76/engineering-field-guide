from dataclasses import dataclass

@dataclass(frozen=True)
class CartLine:
    sku: str
    qty: int

    def __post_init__(self):
        if self.qty <= 0:
            raise ValueError("qty must be > 0")

class Cart:
    """Stores intent (what and how many), not prices — prices are looked up when pricing."""

    def __init__(self, cart_id: str, owner_id: str):        # owner: user id or a guest session id
        self.cart_id, self.owner_id = cart_id, owner_id
        self._lines: dict[str, int] = {}                    # insertion order kept
        self._coupons: dict[str, None] = {}                 # ordered set
        self.version = 0                                    # optimistic locking when persisted

    def add(self, sku: str, qty: int) -> None:
        CartLine(sku, qty)                                  # validates
        self._lines[sku] = self._lines.get(sku, 0) + qty

    def set_quantity(self, sku: str, qty: int) -> None:
        if qty == 0:
            self._lines.pop(sku, None)
        else:
            self._lines[sku] = CartLine(sku, qty).qty

    def remove(self, sku: str) -> None:
        self._lines.pop(sku, None)

    def apply_coupon(self, code: str) -> None:
        self._coupons[code.upper()] = None

    def merge_from(self, guest: "Cart") -> None:
        """Guest cart merged into the user's cart at login: quantities add up."""
        for line in guest.lines():
            self.add(line.sku, line.qty)
        self._coupons.update(dict.fromkeys(guest.coupons()))

    def lines(self) -> list[CartLine]:
        return [CartLine(s, q) for s, q in self._lines.items()]

    def coupons(self) -> list[str]:
        return list(self._coupons)
