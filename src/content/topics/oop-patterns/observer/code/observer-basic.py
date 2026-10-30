from collections import defaultdict
from typing import Callable

Listener = Callable[[str, float | None, float], None]


class StockTicker:
    def __init__(self):
        self._listeners: dict[str, list[Listener]] = defaultdict(list)
        self._prices: dict[str, float] = {}

    def subscribe(self, symbol: str, listener: Listener) -> Callable[[], None]:
        self._listeners[symbol].append(listener)
        return lambda: self._listeners[symbol].remove(listener)  # unsubscribe handle

    def update(self, symbol: str, price: float) -> None:
        old = self._prices.get(symbol)
        self._prices[symbol] = price
        for listener in list(self._listeners[symbol]):  # copy: listeners may unsubscribe
            listener(symbol, old, price)
