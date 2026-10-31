/** Keeps per-floor, per-size free counts; updated by events, never by scanning. */
final class DisplayBoard implements SpotListener {
    private final Map<Integer, Map<SpotSize, AtomicInteger>> free = new ConcurrentHashMap<>();

    DisplayBoard(List<ParkingSpot> spots) {
        for (ParkingSpot s : spots) {
            if (s.isFree()) counter(s).incrementAndGet();
        }
    }

    @Override
    public void spotChanged(ParkingSpot s, boolean nowFree) {
        if (nowFree) counter(s).incrementAndGet();
        else counter(s).decrementAndGet();
        render(s.floor());
    }

    int freeOn(int floor, SpotSize size) {
        return counter(floor, size).get();
    }

    private AtomicInteger counter(ParkingSpot s) {
        return counter(s.floor(), s.size());
    }

    private AtomicInteger counter(int floor, SpotSize size) {
        return free.computeIfAbsent(floor, f -> new ConcurrentHashMap<>())
                .computeIfAbsent(size, z -> new AtomicInteger());
    }

    private void render(int floor) {
        /* push to the physical board / websocket */
    }
}
