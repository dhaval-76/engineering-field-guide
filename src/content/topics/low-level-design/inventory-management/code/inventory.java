record StockKey(String warehouse, String sku) {}

record Reservation(String id, String orderId, StockKey key, int qty, long expiresAt) {}

/**
 * Picks a warehouse that can fill the whole quantity — Strategy, so "nearest", "cheapest" etc. plug
 * in.
 */
interface AllocationStrategy {
    Optional<String> choose(String sku, int qty, Map<String, Integer> availableByWarehouse);
}

class MostStockFirst implements AllocationStrategy {
    public Optional<String> choose(String sku, int qty, Map<String, Integer> available) {
        return available.entrySet().stream()
                .filter(e -> e.getValue() >= qty)
                .max(
                        Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                                // tie: alphabetical
                                .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey);
    }
}

interface LowStockListener {
    void onLowStock(String sku, int totalAvailable);
}

class InventoryService {
    private static final class Level {
        int onHand;
        int reserved;

        int available() {
            return onHand - reserved;
        }
    }

    private final Map<StockKey, Level> levels = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Map<String, Integer> reorderPoint = new HashMap<>();
    private final List<LowStockListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> movements = new ArrayList<>(); // audit trail
    private final AllocationStrategy strategy;

    InventoryService(AllocationStrategy strategy) {
        this.strategy = strategy;
    }

    synchronized void receive(String warehouse, String sku, int qty) { // goods arrive
        level(warehouse, sku).onHand += qty;
        movements.add("RECEIVE %s %s +%d".formatted(warehouse, sku, qty));
    }

    /** Reserve for an order (e.g. at checkout). Expires unless confirmed. */
    synchronized Optional<Reservation> reserve(
            String orderId, String sku, int qty, long now, long ttlMillis) {
        expire(now);
        Map<String, Integer> available = new HashMap<>();
        levels.forEach(
                (k, l) -> {
                    if (k.sku().equals(sku)) available.put(k.warehouse(), l.available());
                });
        Optional<String> wh = strategy.choose(sku, qty, available);
        if (wh.isEmpty()) return Optional.empty(); // never oversell
        StockKey key = new StockKey(wh.get(), sku);
        levels.get(key).reserved += qty;
        Reservation r =
                new Reservation(UUID.randomUUID().toString(), orderId, key, qty, now + ttlMillis);
        reservations.put(r.id(), r);
        movements.add("RESERVE %s %s %d for %s".formatted(key.warehouse(), sku, qty, orderId));
        checkReorder(sku);
        return Optional.of(r);
    }

    /** Payment succeeded and the item ships: stock physically leaves. */
    synchronized boolean confirm(String reservationId, long now) {
        expire(now);
        Reservation r = reservations.remove(reservationId);
        // expired or unknown: re-reserve
        if (r == null) return false;
        Level l = levels.get(r.key());
        l.reserved -= r.qty();
        l.onHand -= r.qty();
        movements.add(
                "SHIP %s %s -%d for %s"
                        .formatted(r.key().warehouse(), r.key().sku(), r.qty(), r.orderId()));
        return true;
    }

    synchronized void cancel(String reservationId) {
        Reservation r = reservations.remove(reservationId);
        if (r != null) release(r, "CANCEL");
    }

    synchronized int available(String sku) {
        return levels.entrySet().stream()
                .filter(e -> e.getKey().sku().equals(sku))
                .mapToInt(e -> e.getValue().available())
                .sum();
    }

    synchronized void setReorderPoint(String sku, int point) {
        reorderPoint.put(sku, point);
    }

    void addListener(LowStockListener l) {
        listeners.add(l);
    }

    synchronized List<String> movements() {
        return List.copyOf(movements);
    }

    // lazily free abandoned carts
    private void expire(long now) {
        reservations
                .values()
                .removeIf(
                        r -> {
                            if (r.expiresAt() > now) return false;
                            release(r, "EXPIRE");
                            return true;
                        });
    }

    private void release(Reservation r, String why) {
        levels.get(r.key()).reserved -= r.qty();
        movements.add(
                "%s %s %s %d for %s"
                        .formatted(why, r.key().warehouse(), r.key().sku(), r.qty(), r.orderId()));
    }

    private void checkReorder(String sku) {
        int total = available(sku);
        Integer point = reorderPoint.get(sku);
        // Observer
        if (point != null && total <= point) listeners.forEach(l -> l.onLowStock(sku, total));
    }

    private Level level(String warehouse, String sku) {
        return levels.computeIfAbsent(new StockKey(warehouse, sku), k -> new Level());
    }
}
