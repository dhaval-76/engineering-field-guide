# uses OrderBook, Order, Side, OrderType from above
import zlib
from concurrent.futures import Future, ThreadPoolExecutor


class MatchingEngine:
    """Each symbol is owned by exactly one single-threaded shard: no locks in the book, deterministic order."""

    def __init__(self, shard_count: int, trade_sink):
        self._shards = [ThreadPoolExecutor(max_workers=1) for _ in range(shard_count)]
        self._books: dict[str, OrderBook] = {}
        self._trade_sink = trade_sink

    def submit(self, o: "Order") -> Future:
        # same symbol → same thread
        shard = self._shards[zlib.crc32(o.symbol.encode()) % len(self._shards)]
        return shard.submit(self._match, o)

    def _match(self, o: "Order"):
        # only this shard touches this symbol
        book = self._books.setdefault(o.symbol, OrderBook(o.symbol))
        trades = book.submit(o)
        for t in trades:
            self._trade_sink(t)
        return trades

    def close(self) -> None:
        for s in self._shards:
            s.shutdown(wait=True)


def risk_allows(o: "Order", available_cash: int, sellable_qty: int, last_price: int) -> bool:
    """Pre-trade risk check before an order ever reaches the book."""
    # 5% buffer for market buys
    price = -(-last_price * 105 // 100) if o.type is OrderType.MARKET else o.limit_ticks
    return (
        available_cash >= price * o.remaining if o.side is Side.BUY else sellable_qty >= o.remaining
    )
