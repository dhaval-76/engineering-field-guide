record Partner(String id, Location location, boolean available, double rating, int activeOrders) {}

/** Strategy: score candidate partners; lower is better. */
interface DispatchStrategy { Optional<Partner> choose(Order order, List<Partner> candidates); }

class NearestAvailable implements DispatchStrategy {
    private final double maxKm;
    NearestAvailable(double maxKm) { this.maxKm = maxKm; }

    public Optional<Partner> choose(Order order, List<Partner> candidates) {
        return candidates.stream()
                .filter(p -> p.available() && p.activeOrders() == 0)
                .filter(p -> p.location().km(order.pickup) <= maxKm)
                .min(Comparator.comparingDouble((Partner p) -> p.location().km(order.pickup))
                        .thenComparing(Comparator.comparingDouble(Partner::rating).reversed()));
    }
}

/** Assign when the restaurant accepts, so the partner arrives as the food gets ready. */
class Dispatcher implements OrderListener {
    private final DispatchStrategy strategy;
    // from a geo index in production
    private final Supplier<List<Partner>> partnersNearby;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();

    Dispatcher(DispatchStrategy strategy, Supplier<List<Partner>> partnersNearby) {
        this.strategy = strategy; this.partnersNearby = partnersNearby;
    }

    public void onStatus(Order order, Status from, Status to) {
        if (to == Status.ACCEPTED) {
            List<Partner> free = partnersNearby.get().stream().filter(p -> !busy.contains(p.id())).toList();
            strategy.choose(order, free).ifPresent(p -> {
                // claim atomically; retry loop omitted
                if (busy.add(p.id())) order.assign(p.id());
            });
        }
        if ((to == Status.DELIVERED || to == Status.CANCELLED) && order.partnerId() != null) busy.remove(order.partnerId());
    }
}

/**
 * Pricing is another strategy: base + distance + surge, minus discount, never below zero.
 */
long deliveryFeePaise(Order o, double surgeMultiplier, long discountPaise) {
    double km = o.pickup.km(o.drop);
    // ₹20 for the first 2 km, ₹8/km after
    long base = 2_000 + Math.round(Math.max(0, km - 2) * 800);
    return Math.max(0, Math.round(base * surgeMultiplier) - discountPaise);
}
