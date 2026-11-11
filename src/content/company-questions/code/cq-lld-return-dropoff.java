record Store(String id, double lat, double lon, int slotsPerWindow) {}

record Booking(String code, String customerId, String storeId, int window) {}

/** Nearest stores from a grid index: only cells around the customer are searched. */
class StoreLocator {
    private final double cellDegrees;
    private final Map<String, List<Store>> cells = new HashMap<>();

    StoreLocator(Collection<Store> stores, double cellDegrees) {
        this.cellDegrees = cellDegrees;
        for (Store s : stores)
            cells.computeIfAbsent(cell(s.lat(), s.lon()), k -> new ArrayList<>()).add(s);
    }

    private String cell(double lat, double lon) {
        return (long) Math.floor(lat / cellDegrees) + ":" + (long) Math.floor(lon / cellDegrees);
    }

    List<Store> nearest(double lat, double lon, int k) {
        List<Store> found = new ArrayList<>();
        long cx = (long) Math.floor(lat / cellDegrees), cy = (long) Math.floor(lon / cellDegrees);
        for (int ring = 0; ring <= 3 && found.size() < k; ring++) { // widen until enough
            found.clear();
            for (long x = cx - ring; x <= cx + ring; x++)
                for (long y = cy - ring; y <= cy + ring; y++)
                    found.addAll(cells.getOrDefault(x + ":" + y, List.of()));
        }
        found.sort(Comparator.comparingDouble(s -> Math.hypot(s.lat() - lat, s.lon() - lon)));
        return found.subList(0, Math.min(k, found.size()));
    }
}

class DropOffService {
    private final Map<String, Store> stores;
    private final Map<String, AtomicInteger> used = new ConcurrentHashMap<>(); // store:window
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong codes = new AtomicLong(1000);

    DropOffService(Collection<Store> all) {
        stores = new HashMap<>();
        for (Store s : all) stores.put(s.id(), s);
    }

    /** Atomic: two customers can't both take the last slot in a window. */
    Optional<Booking> book(String customerId, String storeId, int window) {
        Store s = stores.get(storeId);
        AtomicInteger count =
                used.computeIfAbsent(storeId + ":" + window, k -> new AtomicInteger());
        if (count.incrementAndGet() > s.slotsPerWindow()) {
            count.decrementAndGet();
            return Optional.empty();
        }
        Booking b = new Booking("D" + codes.incrementAndGet(), customerId, storeId, window);
        bookings.put(b.code(), b);
        return Optional.of(b);
    }

    void cancel(String code) {
        Booking b = bookings.remove(code);
        if (b != null) used.get(b.storeId() + ":" + b.window()).decrementAndGet();
    }

    /** At the store: the code proves the booking; the package is then received once. */
    boolean dropOff(String code, String storeId) {
        Booking b = bookings.get(code);
        return b != null && b.storeId().equals(storeId) && bookings.remove(code, b);
    }
}
