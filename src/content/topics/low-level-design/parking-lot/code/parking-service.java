interface SpotSelector {                                      // Strategy: which free spot to try first
    Stream<ParkingSpot> candidates(Vehicle v, List<ParkingSpot> spots);
}

/** Lowest floor first, smallest fitting size first. */
final class NearestSmallestFit implements SpotSelector {
    public Stream<ParkingSpot> candidates(Vehicle v, List<ParkingSpot> spots) {
        List<SpotSize> fits = SpotRules.fitsIn(v.type());
        return spots.stream()
                .filter(s -> fits.contains(s.size()) && s.isFree())
                .sorted(Comparator.comparingInt(ParkingSpot::floor).thenComparingInt(s -> fits.indexOf(s.size())));
    }
}

interface PricingPolicy { Money feeFor(Ticket t, Instant exit); }   // Strategy: how much to charge

final class HourlyWithDailyCap implements PricingPolicy {
    private final Map<VehicleType, Money> hourly;
    private final Map<VehicleType, Money> dailyCap;
    HourlyWithDailyCap(Map<VehicleType, Money> hourly, Map<VehicleType, Money> dailyCap) {
        this.hourly = hourly; this.dailyCap = dailyCap;
    }
    public Money feeFor(Ticket t, Instant exit) {
        long minutes = Math.max(1, Duration.between(t.entry(), exit).toMinutes());
        long hours = (minutes + 59) / 60;                                     // started hours are charged
        long days = hours / 24, rest = hours % 24;
        VehicleType type = t.vehicle().type();
        Money restFee = hourly.get(type).times(rest).min(dailyCap.get(type));
        return dailyCap.get(type).times(days).plus(restFee);
    }
}

interface SpotListener { void spotChanged(ParkingSpot spot, boolean nowFree); }   // Observer: display boards

final class ParkingLot {
    private final List<ParkingSpot> spots;
    private final SpotSelector selector;
    private final PricingPolicy pricing;
    private final Clock clock;
    private final Map<String, Ticket> active = new ConcurrentHashMap<>();
    private final List<SpotListener> listeners = new CopyOnWriteArrayList<>();

    ParkingLot(List<ParkingSpot> spots, SpotSelector selector, PricingPolicy pricing, Clock clock) {
        this.spots = List.copyOf(spots); this.selector = selector; this.pricing = pricing; this.clock = clock;
    }

    void addListener(SpotListener l) { listeners.add(l); }

    /** Tries candidates in order; a lost CAS race just moves on to the next spot. */
    public Optional<Ticket> park(Vehicle v) {
        Iterator<ParkingSpot> it = selector.candidates(v, spots).iterator();
        while (it.hasNext()) {
            ParkingSpot spot = it.next();
            if (spot.tryOccupy(v)) {
                Ticket t = new Ticket(UUID.randomUUID().toString(), v, spot, clock.instant());
                active.put(t.id(), t);
                listeners.forEach(l -> l.spotChanged(spot, false));
                return Optional.of(t);
            }
        }
        return Optional.empty();                                         // full for this vehicle type
    }

    public Money quote(String ticketId) {
        return pricing.feeFor(ticket(ticketId), clock.instant());
    }

    public void pay(String ticketId, Money amount) {
        Ticket t = ticket(ticketId);
        Money due = pricing.feeFor(t, clock.instant());
        if (amount.compareTo(due) < 0) throw new IllegalArgumentException("due " + due);
        t.markPaid(due);                                                 // throws if already paid
    }

    public void exit(String ticketId) {
        Ticket t = ticket(ticketId);
        t.close();                                                       // throws unless paid
        active.remove(ticketId);
        t.spot().release();
        listeners.forEach(l -> l.spotChanged(t.spot(), true));
    }

    private Ticket ticket(String id) {
        Ticket t = active.get(id);
        if (t == null) throw new NoSuchElementException("unknown ticket " + id);
        return t;
    }
}
