enum Direction {
    UP,
    DOWN,
    IDLE
}

class Elevator {
    final int id;
    int floor;
    Direction direction = Direction.IDLE;
    private final TreeSet<Integer> stops = new TreeSet<>();

    Elevator(int id, int floor) {
        this.id = id;
        this.floor = floor;
    }

    void addStop(int f) {
        stops.add(f);
        if (direction == Direction.IDLE) direction = f >= floor ? Direction.UP : Direction.DOWN;
    }

    boolean hasStops() {
        return !stops.isEmpty();
    }

    /** One tick: move a floor; keep going the same way while stops remain ahead (SCAN). */
    void step() {
        if (stops.isEmpty()) {
            direction = Direction.IDLE;
            return;
        }
        if (stops.remove(floor)) return; // doors open at this floor this tick
        Integer ahead = direction == Direction.UP ? stops.ceiling(floor) : stops.floor(floor);
        if (ahead == null) direction = direction == Direction.UP ? Direction.DOWN : Direction.UP;
        floor += direction == Direction.UP ? 1 : -1;
    }
}

/** Strategy: swap the dispatch policy without touching the elevators. */
interface DispatchStrategy {
    Elevator choose(List<Elevator> elevators, int floor, Direction wanted);
}

class NearestSuitable implements DispatchStrategy {
    public Elevator choose(List<Elevator> elevators, int floor, Direction wanted) {
        Elevator best = null;
        int bestCost = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            boolean onTheWay =
                    e.direction == wanted
                            && (wanted == Direction.UP ? e.floor <= floor : e.floor >= floor);
            int cost =
                    Math.abs(e.floor - floor)
                            + (e.direction == Direction.IDLE || onTheWay
                                    ? 0
                                    : 100); // avoid U-turns
            if (cost < bestCost) {
                bestCost = cost;
                best = e;
            }
        }
        return best;
    }
}

class ElevatorController {
    final List<Elevator> elevators = new ArrayList<>();
    private final DispatchStrategy strategy;

    ElevatorController(int count, DispatchStrategy strategy) {
        for (int i = 0; i < count; i++) elevators.add(new Elevator(i, 0));
        this.strategy = strategy;
    }

    Elevator hallCall(int floor, Direction wanted) { // button on a floor
        Elevator e = strategy.choose(elevators, floor, wanted);
        e.addStop(floor);
        return e;
    }

    void carCall(int elevatorId, int floor) {
        elevators.get(elevatorId).addStop(floor);
    }

    void tick() {
        elevators.forEach(Elevator::step);
    }
}
