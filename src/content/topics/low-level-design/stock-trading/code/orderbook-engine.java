/**
 * One order book per symbol. Called from a single thread per symbol, so no locks are needed inside.
 */
final class OrderBook {
    final String symbol;
    // Price levels: best bid = highest price, best ask = lowest. FIFO queue within a
    // level = time priority.
    private final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    // O(1) cancel lookup
    private final Map<String, Order> byId = new HashMap<>();
    private long tradeSeq;

    OrderBook(String symbol) {
        this.symbol = symbol;
    }

    List<Trade> submit(Order incoming) {
        List<Trade> trades = new ArrayList<>();
        TreeMap<Long, ArrayDeque<Order>> opposite = incoming.side == Side.BUY ? asks : bids;

        while (incoming.remaining > 0 && !opposite.isEmpty()) {
            long bestPrice = opposite.firstKey();
            if (incoming.type == OrderType.LIMIT && !crosses(incoming, bestPrice)) break;
            ArrayDeque<Order> level = opposite.get(bestPrice);
            Order resting = level.peekFirst();
            long qty = Math.min(incoming.remaining, resting.remaining);

            // Trade happens at the RESTING order's price (the one that was in the book
            // first).
            trades.add(
                    incoming.side == Side.BUY
                            ? new Trade(symbol, incoming.id, resting.id, bestPrice, qty, ++tradeSeq)
                            : new Trade(
                                    symbol, resting.id, incoming.id, bestPrice, qty, ++tradeSeq));
            incoming.remaining -= qty;
            resting.remaining -= qty;
            resting.status =
                    resting.remaining == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
            if (resting.remaining == 0) {
                level.pollFirst();
                byId.remove(resting.id);
                if (level.isEmpty()) opposite.remove(bestPrice);
            }
        }

        if (incoming.remaining == 0) incoming.status = OrderStatus.FILLED;
        // unfilled market qty never rests
        else if (incoming.type == OrderType.MARKET) incoming.status = OrderStatus.CANCELLED;
        else {
            incoming.status = trades.isEmpty() ? OrderStatus.OPEN : OrderStatus.PARTIALLY_FILLED;
            rest(incoming);
        }
        return trades;
    }

    boolean cancel(String orderId) {
        Order o = byId.remove(orderId);
        // already filled or unknown
        if (o == null) return false;
        TreeMap<Long, ArrayDeque<Order>> side = o.side == Side.BUY ? bids : asks;
        ArrayDeque<Order> level = side.get(o.limitTicks);
        // O(level size); fine, or use a linked list node
        level.remove(o);
        if (level.isEmpty()) side.remove(o.limitTicks);
        o.status = OrderStatus.CANCELLED;
        return true;
    }

    private static boolean crosses(Order o, long bestOpposite) {
        return o.side == Side.BUY ? o.limitTicks >= bestOpposite : o.limitTicks <= bestOpposite;
    }

    private void rest(Order o) {
        (o.side == Side.BUY ? bids : asks)
                .computeIfAbsent(o.limitTicks, p -> new ArrayDeque<>())
                .addLast(o);
        byId.put(o.id, o);
    }

    OptionalLong bestBid() {
        return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey());
    }

    OptionalLong bestAsk() {
        return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey());
    }
}
