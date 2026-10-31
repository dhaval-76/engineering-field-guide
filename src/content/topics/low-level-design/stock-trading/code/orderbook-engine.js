// Compact JS version: arrays sorted by price then sequence (fine for teaching; use a
// tree/heap for speed).
export class OrderBook {
  bids = []; // sorted: price desc, seq asc
  asks = []; // sorted: price asc, seq asc
  #seq = 0;

  submit(order) {
    order = { ...order, seq: ++this.#seq, remaining: order.qty };
    const opposite = order.side === 'BUY' ? this.asks : this.bids;
    const crosses = (p) => (order.side === 'BUY' ? order.limit >= p : order.limit <= p);
    const trades = [];
    while (order.remaining > 0 && opposite.length) {
      const best = opposite[0];
      if (order.type === 'LIMIT' && !crosses(best.limit)) break;
      const qty = Math.min(order.remaining, best.remaining);
      trades.push({
        price: best.limit,
        qty,
        buy: order.side === 'BUY' ? order.id : best.id,
        sell: order.side === 'SELL' ? order.id : best.id,
      });
      order.remaining -= qty;
      best.remaining -= qty;
      if (best.remaining === 0) opposite.shift();
    }
    if (order.remaining > 0 && order.type === 'LIMIT') {
      const book = order.side === 'BUY' ? this.bids : this.asks;
      book.push(order);
      book.sort(
        order.side === 'BUY'
          ? (a, b) => b.limit - a.limit || a.seq - b.seq
          : (a, b) => a.limit - b.limit || a.seq - b.seq,
      );
    }
    return trades;
  }
}
