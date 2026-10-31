enum Status { PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED, REJECTED }
enum Actor { CUSTOMER, RESTAURANT, PARTNER, SYSTEM }

record Transition(Status from, Status to, Set<Actor> allowed) {}

/** The order lifecycle as data: which moves exist and who may make them. */
final class OrderLifecycle {
    private static final List<Transition> RULES = List.of(
            new Transition(Status.PLACED, Status.ACCEPTED, Set.of(Actor.RESTAURANT)),
            // timeout
            new Transition(Status.PLACED, Status.REJECTED, Set.of(Actor.RESTAURANT, Actor.SYSTEM)),
            // free cancel
            new Transition(Status.PLACED, Status.CANCELLED, Set.of(Actor.CUSTOMER)),
            new Transition(Status.ACCEPTED, Status.PREPARING, Set.of(Actor.RESTAURANT)),
            // fee may apply
            new Transition(Status.ACCEPTED, Status.CANCELLED, Set.of(Actor.CUSTOMER, Actor.SYSTEM)),
            new Transition(Status.PREPARING, Status.READY, Set.of(Actor.RESTAURANT)),
            new Transition(Status.READY, Status.PICKED_UP, Set.of(Actor.PARTNER)),
            new Transition(Status.PICKED_UP, Status.DELIVERED, Set.of(Actor.PARTNER)));

    static boolean allowed(Status from, Status to, Actor actor) {
        return RULES.stream().anyMatch(r -> r.from() == from && r.to() == to && r.allowed().contains(actor));
    }
}

record Location(double lat, double lng) {
    double km(Location o) {                                          // haversine distance
        double dLat = Math.toRadians(o.lat - lat), dLng = Math.toRadians(o.lng - lng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(o.lat)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

// push notifications, ETA, analytics
interface OrderListener { void onStatus(Order order, Status from, Status to); }

class Order {
    final String id, restaurantId;
    final Location pickup, drop;
    final long totalPaise;
    private Status status = Status.PLACED;
    private String partnerId;
    private final List<String> history = new ArrayList<>();
    private final List<OrderListener> listeners;

    Order(String id, String restaurantId, Location pickup, Location drop, long totalPaise, List<OrderListener> listeners) {
        this.id = id; this.restaurantId = restaurantId; this.pickup = pickup; this.drop = drop;
        this.totalPaise = totalPaise; this.listeners = listeners;
        history.add("PLACED");
    }

    synchronized void moveTo(Status next, Actor actor) {
        if (!OrderLifecycle.allowed(status, next, actor))
            throw new IllegalStateException(actor + " cannot move order " + id + " from " + status + " to " + next);
        if (next == Status.PICKED_UP && partnerId == null) throw new IllegalStateException("no partner assigned");
        Status prev = status;
        status = next;
        history.add(next.name());
        listeners.forEach(l -> l.onStatus(this, prev, next));
    }

    synchronized Status status() { return status; }
    synchronized void assign(String partnerId) { this.partnerId = partnerId; }
    synchronized String partnerId() { return partnerId; }
    synchronized List<String> history() { return List.copyOf(history); }
}
