record HallCall(int floor, Direction wants) {}

interface DispatchStrategy {
    ElevatorCar choose(HallCall call, List<ElevatorCar> cars);
}

/** Cost = floors to travel before reaching the caller, given each car's current direction. */
final class NearestSuitableCar implements DispatchStrategy {
    private final int topFloor;

    NearestSuitableCar(int topFloor) {
        this.topFloor = topFloor;
    }

    public ElevatorCar choose(HallCall call, List<ElevatorCar> cars) {
        return cars.stream()
                .min(
                        Comparator.comparingInt((ElevatorCar c) -> cost(c, call))
                                .thenComparingInt(ElevatorCar::pendingStops))
                .orElseThrow();
    }

    private int cost(ElevatorCar car, HallCall call) {
        int here = car.floor(), target = call.floor();
        return switch (car.direction()) {
            case IDLE -> Math.abs(here - target);
            case UP ->
                    (target >= here && call.wants() == Direction.UP)
                            ? target - here // on the way
                            // finish the sweep, then come back
                            : (topFloor - here) + Math.abs(topFloor - target);
            case DOWN ->
                    (target <= here && call.wants() == Direction.DOWN)
                            ? here - target
                            // sweep to ground (0), then up
                            : here + target;
        };
    }
}

final class ElevatorController {
    private final List<ElevatorCar> cars;
    private final DispatchStrategy strategy;

    ElevatorController(List<ElevatorCar> cars, DispatchStrategy strategy) {
        this.cars = cars;
        this.strategy = strategy;
    }

    void hallCall(int floor, Direction wants) {
        strategy.choose(new HallCall(floor, wants), cars).addStop(floor);
    }

    void carCall(int carId, int floor) {
        cars.get(carId).addStop(floor);
    }

    void tick() {
        cars.forEach(ElevatorCar::tick);
    }
}
