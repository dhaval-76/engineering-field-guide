enum DriverStatus { OFFLINE, AVAILABLE, OFFERED, ON_TRIP }

final class Driver {
    final String id;
    final VehicleClass vehicle;
    final double rating;
    volatile Location location;
    private final AtomicReference<DriverStatus> status = new AtomicReference<>(DriverStatus.OFFLINE);

    Driver(String id, VehicleClass vehicle, double rating) { this.id = id; this.vehicle = vehicle; this.rating = rating; }

    /** Only one ride can reserve a driver: AVAILABLE → OFFERED is a compare-and-set. */
    boolean tryReserve() { return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.OFFERED); }
    void release()       { status.set(DriverStatus.AVAILABLE); }
    void startTrip()     { status.set(DriverStatus.ON_TRIP); }
    DriverStatus status() { return status.get(); }
}

interface DriverIndex { List<Driver> near(Location p, double radiusKm); }   // grid / geohash / quadtree

interface MatchingStrategy { List<Driver> rank(Ride ride, List<Driver> candidates); }

/** Score: closer is better, rating breaks near-ties. Weights are tuning knobs. */
final class NearestGoodDriver implements MatchingStrategy {
    public List<Driver> rank(Ride ride, List<Driver> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble((Driver d) -> d.location.distanceKm(ride.pickup()) - 0.3 * d.rating))
                .toList();
    }
}

final class Dispatcher {
    private final DriverIndex index;
    private final MatchingStrategy strategy;
    private final OfferChannel offers;                         // push notification to the driver app
    private final Duration offerTimeout = Duration.ofSeconds(15);

    Dispatcher(DriverIndex index, MatchingStrategy strategy, OfferChannel offers) {
        this.index = index; this.strategy = strategy; this.offers = offers;
    }

    /** Widen the radius until someone accepts or we give up. */
    Optional<Driver> match(Ride ride, VehicleClass wanted, Clock clock) {
        for (double radius : new double[]{1, 2, 4, 8}) {
            List<Driver> candidates = index.near(ride.pickup(), radius).stream()
                    .filter(d -> d.vehicle == wanted && d.status() == DriverStatus.AVAILABLE).toList();
            for (Driver d : strategy.rank(ride, candidates)) {
                if (!d.tryReserve()) continue;                  // another ride got this driver first
                if (offers.offerAndAwait(d, ride, offerTimeout)) {
                    ride.assign(d.id, clock.instant());
                    return Optional.of(d);
                }
                d.release();                                    // declined or timed out
            }
        }
        return Optional.empty();
    }
}
