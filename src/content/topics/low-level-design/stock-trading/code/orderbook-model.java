enum Side { BUY, SELL }
enum OrderType { LIMIT, MARKET }
enum OrderStatus { OPEN, PARTIALLY_FILLED, FILLED, CANCELLED }

/** Prices are integer ticks (e.g. paise), never double — 0.1 + 0.2 must equal 0.3. */
final class Order {
    final String id, userId, symbol;
    final Side side;
    final OrderType type;
    final long limitTicks;          // ignored for MARKET
    final long seq;                 // arrival sequence = time priority
    long remaining;
    OrderStatus status = OrderStatus.OPEN;

    Order(String id, String userId, String symbol, Side side, OrderType type, long limitTicks, long qty, long seq) {
        this.id = id; this.userId = userId; this.symbol = symbol; this.side = side; this.type = type;
        this.limitTicks = limitTicks; this.remaining = qty; this.seq = seq;
    }
}

record Trade(String symbol, String buyOrderId, String sellOrderId, long priceTicks, long qty, long seq) {}
