/**
 * O(log n) allocation instead of scanning every spot: one sorted set of free spots per
 * size. The set is ordered by "distance" (floor, then spot number), so pollFirst()
 * returns the nearest.
 */
final class FreeSpotIndex {
    private final Map<SpotSize, ConcurrentSkipListSet<ParkingSpot>> free = new EnumMap<>(SpotSize.class);
    private static final Comparator<ParkingSpot> NEAREST =
            Comparator.comparingInt(ParkingSpot::floor).thenComparing(ParkingSpot::id);

    FreeSpotIndex(List<ParkingSpot> spots) {
        for (SpotSize z : SpotSize.values()) free.put(z, new ConcurrentSkipListSet<>(NEAREST));
        spots.forEach(s -> free.get(s.size()).add(s));
    }

    /** pollFirst() removes atomically, so two threads can never receive the same spot. */
    Optional<ParkingSpot> claim(VehicleType type) {
        for (SpotSize z : SpotRules.fitsIn(type)) {
            ParkingSpot s = free.get(z).pollFirst();
            if (s != null) return Optional.of(s);
        }
        return Optional.empty();
    }

    void giveBack(ParkingSpot s) { free.get(s.size()).add(s); }
}
