record Reservation(String id, String sku, int qty, long expiresAtMillis) {}

/**
 * Stock is split into available and reserved. Reserving is an atomic conditional update; a
 * reservation either becomes a sale or expires and returns its units. In a database the same step
 * is one statement: UPDATE stock SET available = available - ? WHERE sku = ? AND available >= ? and
 * "1 row updated" means you got it.
 */
class InventoryService {
    private final Map<String, Integer> available = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Object lock = new Object(); // one lock is enough here; shard by sku to scale
    private final AtomicLong ids = new AtomicLong();
    private final LongSupplier clock;

    InventoryService(LongSupplier clock) {
        this.clock = clock;
    }

    void restock(String sku, int qty) {
        synchronized (lock) {
            available.merge(sku, qty, Integer::sum);
        }
    }

    Optional<Reservation> reserve(String sku, int qty, long holdMillis) {
        synchronized (lock) {
            expireOld();
            int have = available.getOrDefault(sku, 0);
            if (have < qty) return Optional.empty(); // the second buyer of the last unit
            available.put(sku, have - qty);
            Reservation r =
                    new Reservation(
                            "r" + ids.incrementAndGet(), sku, qty, clock.getAsLong() + holdMillis);
            reservations.put(r.id(), r);
            return Optional.of(r);
        }
    }

    boolean confirm(String reservationId) { // payment succeeded: the units are sold
        synchronized (lock) {
            expireOld();
            return reservations.remove(reservationId) != null;
        }
    }

    void release(String reservationId) { // payment failed or cart abandoned
        synchronized (lock) {
            Reservation r = reservations.remove(reservationId);
            if (r != null) available.merge(r.sku(), r.qty(), Integer::sum);
        }
    }

    int available(String sku) {
        synchronized (lock) {
            expireOld();
            return available.getOrDefault(sku, 0);
        }
    }

    private void expireOld() {
        long now = clock.getAsLong();
        for (var it = reservations.values().iterator(); it.hasNext(); ) {
            Reservation r = it.next();
            if (r.expiresAtMillis() <= now) {
                available.merge(r.sku(), r.qty(), Integer::sum);
                it.remove();
            }
        }
    }
}
