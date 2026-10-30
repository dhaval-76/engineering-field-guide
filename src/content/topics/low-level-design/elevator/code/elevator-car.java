enum Direction { UP, DOWN, IDLE }

/**
 * One car using the LOOK algorithm: keep going in the current direction while there are stops
 * ahead; reverse only when nothing is left ahead. Driven by tick() so it is deterministic to test.
 */
final class ElevatorCar {
    private final int id, minFloor, maxFloor;
    private int floor;
    private Direction direction = Direction.IDLE;
    private final TreeSet<Integer> stops = new TreeSet<>();
    private final Door door = new Door();

    ElevatorCar(int id, int minFloor, int maxFloor, int startFloor) {
        this.id = id; this.minFloor = minFloor; this.maxFloor = maxFloor; this.floor = startFloor;
    }

    synchronized void addStop(int f) {
        if (f < minFloor || f > maxFloor) throw new IllegalArgumentException("no floor " + f);
        if (f == floor && direction == Direction.IDLE) { door.open(); return; }
        stops.add(f);
        if (direction == Direction.IDLE) direction = f > floor ? Direction.UP : Direction.DOWN;
    }

    /** Advance the simulation by one step: move one floor, or service a stop. */
    synchronized void tick() {
        if (door.isOpen()) { door.close(); return; }
        if (stops.remove(floor)) { door.open(); return; }          // arrived at a requested floor
        Integer next = nextStop();
        if (next == null) { direction = Direction.IDLE; return; }
        direction = next > floor ? Direction.UP : Direction.DOWN;
        floor += direction == Direction.UP ? 1 : -1;
    }

    private Integer nextStop() {
        if (stops.isEmpty()) return null;
        Integer ahead = direction == Direction.DOWN ? stops.floor(floor) : stops.ceiling(floor);
        if (ahead != null) return ahead;                            // keep going (LOOK)
        return direction == Direction.DOWN ? stops.ceiling(floor) : stops.floor(floor);   // reverse
    }

    synchronized int floor() { return floor; }
    synchronized Direction direction() { return direction; }
    synchronized int pendingStops() { return stops.size(); }
    int id() { return id; }
}
