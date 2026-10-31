// uses OrderBook from above Node has one thread; the equivalent of "one thread per
// symbol" is a per-symbol promise chain, so orders for a symbol are matched strictly in
// arrival order even if submit() is async upstream.
export class MatchingEngine {
  #books = new Map();
  #tails = new Map(); // symbol -> promise of the last queued submit

  constructor(tradeSink) {
    this.tradeSink = tradeSink; // publish to market data, settlement, portfolios
  }

  submit(order) {
    const prev = this.#tails.get(order.symbol) ?? Promise.resolve();
    const next = prev.then(() => {
      if (!this.#books.has(order.symbol)) this.#books.set(order.symbol, new OrderBook());
      const trades = this.#books.get(order.symbol).submit(order);
      trades.forEach(this.tradeSink);
      return trades;
    });
    // a failed order mustn't block the queue
    this.#tails.set(order.symbol, next.catch(() => {}));
    return next;
  }
}
// To use several cores, shard symbols across worker_threads by hash(symbol) — same idea
// as the Java shards.

// Pre-trade risk check before an order ever reaches the book.
export function riskAllows(order, account, lastPrice) {
  // 5% buffer for market buys
  const price = order.type === 'MARKET' ? Math.ceil((lastPrice * 105) / 100) : order.limit;
  return order.side === 'BUY'
    ? account.availableCash >= price * order.remaining
    : account.sellable(order.symbol) >= order.remaining; // no naked short selling
}
