/**
 * Exchanges serialise each instrument: one thread owns one or more order books, fed by a queue.
 * No locks inside the book, deterministic ordering, and symbols scale across cores.
 */
final class MatchingEngine {
    private final ExecutorService[] shards;
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Consumer<Trade> tradeSink;                 // publish to market data, settlement, portfolios

    MatchingEngine(int shardCount, Consumer<Trade> tradeSink) {
        shards = new ExecutorService[shardCount];
        for (int i = 0; i < shardCount; i++) shards[i] = Executors.newSingleThreadExecutor();
        this.tradeSink = tradeSink;
    }

    CompletableFuture<List<Trade>> submit(Order o) {
        ExecutorService shard = shards[Math.floorMod(o.symbol.hashCode(), shards.length)];   // same symbol → same thread
        return CompletableFuture.supplyAsync(() -> {
            List<Trade> trades = books.computeIfAbsent(o.symbol, OrderBook::new).submit(o);
            trades.forEach(tradeSink);
            return trades;
        }, shard);
    }
}

/** Pre-trade risk check before an order ever reaches the book. */
final class RiskCheck {
    boolean allow(Order o, Account a, long lastPriceTicks) {
        long price = o.type == OrderType.MARKET ? lastPriceTicks * 105 / 100 : o.limitTicks;   // 5% buffer for market buys
        return o.side == Side.BUY
                ? a.availableCashTicks() >= price * o.remaining
                : a.sellableQuantity(o.symbol) >= o.remaining;              // no naked short selling
    }
}
