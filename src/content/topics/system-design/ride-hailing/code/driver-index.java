/** In-memory index of moving drivers for one city shard: frequent updates, expiry, nearest-k by rings. */
class DriverIndex {
    static final double CELL = 0.01;                                   // degrees ≈ 1.1 km of latitude
    private record Driver(double lat, double lon, double seenAt, boolean available) {}
    private record Cell(long y, long x) {}

    private final double staleAfter;
    private final Map<String, Driver> drivers = new HashMap<>();
    private final Map<Cell, Set<String>> cells = new HashMap<>();

    DriverIndex(double staleAfterSeconds) { this.staleAfter = staleAfterSeconds; }

    static Cell cellOf(double lat, double lon) { return new Cell((long) Math.floor(lat / CELL), (long) Math.floor(lon / CELL)); }

    synchronized void update(String id, double lat, double lon, boolean available, double now) {
        Driver old = drivers.get(id);
        if (old != null && !cellOf(old.lat(), old.lon()).equals(cellOf(lat, lon)))
            cells.get(cellOf(old.lat(), old.lon())).remove(id);       // moved to another cell
        drivers.put(id, new Driver(lat, lon, now, available));
        cells.computeIfAbsent(cellOf(lat, lon), c -> new HashSet<>()).add(id);
    }

    /** Atomically take an available driver for one offer, so two riders can't both get them. */
    synchronized boolean claim(String id) {
        Driver d = drivers.get(id);
        if (d == null || !d.available()) return false;
        drivers.put(id, new Driver(d.lat(), d.lon(), d.seenAt(), false));
        return true;
    }

    synchronized int evictStale(double now) {                          // drivers whose app stopped reporting
        List<String> gone = drivers.entrySet().stream()
                .filter(e -> now - e.getValue().seenAt() > staleAfter).map(Map.Entry::getKey).toList();
        for (String id : gone) {
            Driver d = drivers.remove(id);
            cells.get(cellOf(d.lat(), d.lon())).remove(id);
        }
        return gone.size();
    }

    /** Search rings of cells outward until k drivers are found that are closer than any unvisited cell. */
    synchronized List<String> nearest(double lat, double lon, int k, double maxKm, double now) {
        Cell c = cellOf(lat, lon);
        double cellKm = CELL * 111 * Math.max(Math.cos(Math.toRadians(lat)), 0.1);   // narrowest side
        int maxRing = (int) Math.ceil(maxKm / cellKm) + 1;
        List<Map.Entry<Double, String>> found = new ArrayList<>();
        for (int ring = 0; ring <= maxRing; ring++) {
            for (int dy = -ring; dy <= ring; dy++)
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dy), Math.abs(dx)) != ring) continue;          // outer ring only
                    for (String id : cells.getOrDefault(new Cell(c.y() + dy, c.x() + dx), Set.of())) {
                        Driver d = drivers.get(id);
                        if (!d.available() || now - d.seenAt() > staleAfter) continue;
                        double dist = km(lat, lon, d.lat(), d.lon());
                        if (dist <= maxKm) found.add(Map.entry(dist, id));
                    }
                }
            found.sort(Map.Entry.<Double, String>comparingByKey().thenComparing(Map.Entry.comparingByValue()));
            if (found.size() >= k && found.get(k - 1).getKey() <= ring * cellKm) break;
        }
        return found.stream().limit(k).map(Map.Entry::getValue).toList();
    }

    static double km(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(dl / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(a));
    }
}
