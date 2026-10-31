@FunctionalInterface
interface PriceListener {
    void onPriceChange(String symbol, BigDecimal oldPrice, BigDecimal newPrice);
}

final class StockTicker {                                     // the subject
    private final Map<String, List<PriceListener>> listeners = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    /** Returns a handle that unsubscribes — callers can't forget which listener they added. */
    public Runnable subscribe(String symbol, PriceListener l) {
        listeners.computeIfAbsent(symbol, k -> new CopyOnWriteArrayList<>()).add(l);
        return () -> listeners.getOrDefault(symbol, List.of()).remove(l);
    }

    public void update(String symbol, BigDecimal price) {
        BigDecimal old = prices.put(symbol, price);
        for (PriceListener l : listeners.getOrDefault(symbol, List.of())) {
            try {
                l.onPriceChange(symbol, old, price);
            // one bad listener must not stop the rest
            } catch (RuntimeException e) {
                log.warn("listener failed for {}", symbol, e);
            }
        }
    }
}

// The ticker knows nothing about alerts, charts or audit logs:
Runnable stop = ticker.subscribe("INFY", (s, oldP, newP) -> alerts.maybeAlert(s, oldP, newP));
ticker.subscribe("INFY", chart::redraw);
ticker.subscribe("INFY", audit::record);
