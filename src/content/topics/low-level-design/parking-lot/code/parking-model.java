enum VehicleType { MOTORCYCLE, CAR, TRUCK }
enum SpotSize { SMALL, MEDIUM, LARGE }

record Vehicle(String plate, VehicleType type) {}

/** Which spot sizes a vehicle may use, smallest first (so cars don't take truck spots unnecessarily). */
final class SpotRules {
    static List<SpotSize> fitsIn(VehicleType t) {
        return switch (t) {
            case MOTORCYCLE -> List.of(SpotSize.SMALL, SpotSize.MEDIUM, SpotSize.LARGE);
            case CAR        -> List.of(SpotSize.MEDIUM, SpotSize.LARGE);
            case TRUCK      -> List.of(SpotSize.LARGE);
        };
    }
}

final class ParkingSpot {
    private final String id;
    private final int floor;
    private final SpotSize size;
    private final AtomicReference<Vehicle> occupant = new AtomicReference<>();   // null = free

    ParkingSpot(String id, int floor, SpotSize size) { this.id = id; this.floor = floor; this.size = size; }

    /** Atomic claim: exactly one caller can move this spot from free to occupied. */
    boolean tryOccupy(Vehicle v) { return occupant.compareAndSet(null, v); }

    void release() { occupant.set(null); }
    boolean isFree() { return occupant.get() == null; }
    String id() { return id; }
    int floor() { return floor; }
    SpotSize size() { return size; }
}

enum TicketStatus { ACTIVE, PAID, CLOSED }

final class Ticket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entry;
    private TicketStatus status = TicketStatus.ACTIVE;
    private Money fee;

    Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entry) {
        this.id = id; this.vehicle = vehicle; this.spot = spot; this.entry = entry;
    }
    synchronized void markPaid(Money fee) {
        if (status != TicketStatus.ACTIVE) throw new IllegalStateException("ticket " + id + " is " + status);
        this.fee = fee; status = TicketStatus.PAID;
    }
    synchronized void close() {
        if (status != TicketStatus.PAID) throw new IllegalStateException("pay before exit");
        status = TicketStatus.CLOSED;
    }
    String id() { return id; } Vehicle vehicle() { return vehicle; } ParkingSpot spot() { return spot; } Instant entry() { return entry; }
}
