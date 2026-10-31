enum RideStatus {
    REQUESTED,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<RideStatus, Set<RideStatus>> NEXT =
            Map.of(
                    REQUESTED, EnumSet.of(DRIVER_ASSIGNED, CANCELLED),
                    // → REQUESTED: driver backed out, re-match
                    DRIVER_ASSIGNED, EnumSet.of(DRIVER_ARRIVED, REQUESTED, CANCELLED),
                    DRIVER_ARRIVED, EnumSet.of(IN_PROGRESS, CANCELLED),
                    IN_PROGRESS, EnumSet.of(COMPLETED),
                    COMPLETED, EnumSet.noneOf(RideStatus.class),
                    CANCELLED, EnumSet.noneOf(RideStatus.class));

    boolean canMoveTo(RideStatus next) {
        return NEXT.get(this).contains(next);
    }
}

final class Ride {
    private final String id;
    private final String riderId;
    private final Location pickup, drop;
    private RideStatus status = RideStatus.REQUESTED;
    private String driverId;
    private final List<StatusChange> history = new ArrayList<>();

    Ride(String id, String riderId, Location pickup, Location drop) {
        this.id = id;
        this.riderId = riderId;
        this.pickup = pickup;
        this.drop = drop;
    }

    synchronized void transition(RideStatus next, String actor, Instant at) {
        if (!status.canMoveTo(next))
            throw new IllegalStateException(status + " → " + next + " not allowed");
        history.add(new StatusChange(status, next, actor, at));
        status = next;
    }

    synchronized void assign(String driverId, Instant at) {
        this.driverId = driverId;
        transition(RideStatus.DRIVER_ASSIGNED, driverId, at);
    }

    synchronized void unassign(Instant at) {
        transition(RideStatus.REQUESTED, driverId, at);
        driverId = null;
    }

    synchronized RideStatus status() {
        return status;
    }

    Location pickup() {
        return pickup;
    }

    String id() {
        return id;
    }
}
