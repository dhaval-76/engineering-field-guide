from dataclasses import dataclass, field
from enum import Enum

class Side(Enum):
    BUY = "buy"
    SELL = "sell"

class OrderType(Enum):
    LIMIT = "limit"
    MARKET = "market"

@dataclass
class Order:
    """Prices are integer ticks (e.g. paise), never float — 0.1 + 0.2 must equal 0.3."""
    id: str
    user_id: str
    symbol: str
    side: Side
    type: OrderType
    limit_ticks: int | None        # None for MARKET
    qty: int
    seq: int = 0                   # arrival sequence = time priority (assigned by the book)
    remaining: int = field(init=False)
    status: str = "OPEN"           # OPEN | PARTIALLY_FILLED | FILLED | CANCELLED

    def __post_init__(self):
        self.remaining = self.qty

@dataclass(frozen=True)
class Trade:
    symbol: str
    buy_order_id: str
    sell_order_id: str
    price_ticks: int
    qty: int
