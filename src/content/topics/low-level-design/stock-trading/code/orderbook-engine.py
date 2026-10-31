# uses Order, Side, OrderType, Trade from the model above
import heapq
import itertools


class OrderBook:
    """Price-time priority with two heaps: best bid = highest price, best ask = lowest; ties by arrival."""

    def __init__(self, symbol: str):
        self.symbol = symbol
        self.bids: list = []  # (-price, seq, order)
        self.asks: list = []  # (price, seq, order)
        self._seq = itertools.count(1)

    def submit(self, o: "Order") -> list["Trade"]:
        o.seq = next(self._seq)
        buy = o.side is Side.BUY
        opposite = self.asks if buy else self.bids
        trades = []
        while o.remaining and opposite:
            best = opposite[0][2]
            if best.status == "CANCELLED":
                # lazy deletion of cancelled orders
                heapq.heappop(opposite)
                continue
            if o.type is OrderType.LIMIT and (
                o.limit_ticks < best.limit_ticks if buy else o.limit_ticks > best.limit_ticks
            ):
                break  # no longer crosses
            qty = min(o.remaining, best.remaining)
            trades.append(
                Trade(
                    self.symbol,
                    o.id if buy else best.id,
                    best.id if buy else o.id,
                    best.limit_ticks,
                    qty,
                )
            )
            o.remaining -= qty
            best.remaining -= qty
            best.status = "FILLED" if best.remaining == 0 else "PARTIALLY_FILLED"
            if best.remaining == 0:
                heapq.heappop(opposite)
        o.status = "FILLED" if o.remaining == 0 else ("PARTIALLY_FILLED" if trades else "OPEN")
        if o.remaining and o.type is OrderType.LIMIT:  # rest on the book
            heapq.heappush(
                self.bids if buy else self.asks,
                ((-o.limit_ticks if buy else o.limit_ticks), o.seq, o),
            )
        elif o.remaining:
            # market remainder doesn't rest
            o.status = "CANCELLED"
        return trades

    def cancel(self, o: "Order") -> None:
        if o.status in ("OPEN", "PARTIALLY_FILLED"):
            # removed lazily when it reaches the top
            o.status = "CANCELLED"
