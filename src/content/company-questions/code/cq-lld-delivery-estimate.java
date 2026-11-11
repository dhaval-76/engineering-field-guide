enum Speed {
    STANDARD,
    ONE_DAY
}

record EstimateRequest(String pincode, Speed speed, LocalDateTime orderedAt) {}

record Estimate(LocalDate earliest, LocalDate latest, String warehouse) {}

/** Each input is behind an interface: they change independently and can be cached. */
interface WarehouseLocator {
    List<String> warehousesWithStock(String sku);
}

interface TransitTimes {
    Optional<Integer> days(String warehouse, String pincode, Speed speed);
}

interface Calendar {
    boolean isDeliveryDay(LocalDate d);
}

class DeliveryEstimator {
    static final LocalTime CUTOFF = LocalTime.of(14, 0); // orders after 2 pm ship tomorrow
    private final WarehouseLocator locator;
    private final TransitTimes transit;
    private final Calendar calendar;

    DeliveryEstimator(WarehouseLocator l, TransitTimes t, Calendar c) {
        locator = l;
        transit = t;
        calendar = c;
    }

    Optional<Estimate> estimate(String sku, EstimateRequest req) {
        LocalDate ship =
                req.orderedAt().toLocalTime().isAfter(CUTOFF)
                        ? req.orderedAt().toLocalDate().plusDays(1)
                        : req.orderedAt().toLocalDate();
        Estimate best = null;
        for (String w : locator.warehousesWithStock(sku)) {
            Optional<Integer> days = transit.days(w, req.pincode(), req.speed());
            if (days.isEmpty()) continue; // this warehouse can't reach the pincode
            LocalDate earliest = addDeliveryDays(ship, days.get());
            if (best == null || earliest.isBefore(best.earliest())) {
                // promise a range: the late end absorbs one day of delay
                best = new Estimate(earliest, addDeliveryDays(earliest, 1), w);
            }
        }
        return Optional.ofNullable(best);
    }

    private LocalDate addDeliveryDays(LocalDate from, int days) {
        LocalDate d = from;
        for (int added = 0; added < days; ) {
            d = d.plusDays(1);
            if (calendar.isDeliveryDay(d)) added++; // skip Sundays and holidays
        }
        return d;
    }
}
