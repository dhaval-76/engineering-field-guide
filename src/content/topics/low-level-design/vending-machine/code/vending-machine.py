import threading
from dataclasses import dataclass, field
from enum import Enum

ACCEPTED = {1, 2, 5, 10, 20}            # coin denominations (₹)

@dataclass(frozen=True)
class Product:
    code: str
    name: str
    price: int

@dataclass(frozen=True)
class Result:
    message: str
    product: Product | None = None
    change: list[int] = field(default_factory=list)

class State(Enum):
    IDLE = "idle"
    HAS_CREDIT = "has_credit"
    OUT_OF_SERVICE = "out_of_service"

class VendingMachine:
    def __init__(self):
        self.products: dict[str, Product] = {}
        self.stock: dict[str, int] = {}
        self.coin_box: dict[int, int] = {}
        self.inserted: list[int] = []
        self.state = State.IDLE
        self._lock = threading.Lock()

    def insert_coin(self, coin: int) -> bool:
        with self._lock:
            if self.state is State.OUT_OF_SERVICE or coin not in ACCEPTED:
                return False
            self.inserted.append(coin)
            self.state = State.HAS_CREDIT
            return True

    def select(self, code: str) -> Result:
        with self._lock:
            if self.state is not State.HAS_CREDIT:
                return Result("Insert money first")
            p = self.products.get(code)
            if p is None or self.stock.get(code, 0) == 0:
                return Result("Sold out")
            credit = sum(self.inserted)
            if credit < p.price:
                return Result(f"Insert ₹{p.price - credit} more")
            pool = dict(self.coin_box)
            for c in self.inserted:
                pool[c] = pool.get(c, 0) + 1
            change = make_change(credit - p.price, pool)
            if change is None:
                return Result("Exact change only")      # keep credit; customer can cancel

            for c in self.inserted:
                self.coin_box[c] = self.coin_box.get(c, 0) + 1
            coins_out = []
            for coin, n in change.items():
                self.coin_box[coin] -= n
                coins_out += [coin] * n
            self.stock[code] -= 1
            self.inserted.clear()
            self.state = State.IDLE
            return Result(f"Enjoy your {p.name}", p, coins_out)

    def cancel(self) -> list[int]:
        with self._lock:
            refund, self.inserted = self.inserted, []
            if self.state is State.HAS_CREDIT:
                self.state = State.IDLE
            return refund

    def add_product(self, p: Product, quantity: int) -> None:
        self.products[p.code] = p
        self.stock[p.code] = self.stock.get(p.code, 0) + quantity

    def load_coins(self, coin: int, count: int) -> None:
        self.coin_box[coin] = self.coin_box.get(coin, 0) + count

def make_change(amount: int, box: dict[int, int]) -> dict[int, int] | None:
    """Fewest coins for `amount` from a box with limited counts (greedy can fail here)."""
    coins = list(box.items())
    INF = float("inf")
    best = [[INF] * (amount + 1) for _ in range(len(coins) + 1)]
    take = [[0] * (amount + 1) for _ in range(len(coins) + 1)]
    best[0][0] = 0
    for i, (coin, count) in enumerate(coins):
        for a in range(amount + 1):
            for k in range(min(count, a // coin) + 1):
                if best[i][a - k * coin] + k < best[i + 1][a]:
                    best[i + 1][a] = best[i][a - k * coin] + k
                    take[i + 1][a] = k
    if best[len(coins)][amount] == INF:
        return None
    change, a = {}, amount
    for i in range(len(coins), 0, -1):
        k = take[i][a]
        if k:
            change[coins[i - 1][0]] = k
        a -= k * coins[i - 1][0]
    return change
