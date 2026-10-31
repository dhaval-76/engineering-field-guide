/** A uniform grid of ~1 km cells: updating a location is O(1), "nearby" scans the surrounding cells. */
final class GridIndex implements DriverIndex {
    private static final double CELL_DEG = 0.009;               // ≈ 1 km of latitude
    private final Map<Long, Set<Driver>> cells = new ConcurrentHashMap<>();
    private final Map<String, Long> cellOf = new ConcurrentHashMap<>();

    private static long key(int row, int col) { return ((long) row << 32) | (col & 0xffffffffL); }
    private static int row(Location p) { return (int) Math.floor(p.lat() / CELL_DEG); }
    private static int col(Location p) { return (int) Math.floor(p.lng() / CELL_DEG); }

    // called on every GPS ping (every few seconds)
    void update(Driver d, Location p) {
        long newKey = key(row(p), col(p));
        Long oldKey = cellOf.put(d.id, newKey);
        if (oldKey != null && oldKey != newKey) cells.getOrDefault(oldKey, Set.of()).remove(d);
        cells.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet()).add(d);
        d.location = p;
    }

    public List<Driver> near(Location p, double radiusKm) {
        int r = (int) Math.ceil(radiusKm), row = row(p), col = col(p);
        List<Driver> out = new ArrayList<>();
        for (int dr = -r; dr <= r; dr++) for (int dc = -r; dc <= r; dc++) {
            out.addAll(cells.getOrDefault(key(row + dr, col + dc), Set.of()));
        }
        return out.stream().filter(d -> d.location.distanceKm(p) <= radiusKm).toList();
    }
}
