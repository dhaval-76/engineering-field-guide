interface FarePolicy { Money estimate(TripEstimate t, double surge); }

record TripEstimate(VehicleClass vehicle, double km, Duration duration) {}

final class StandardFare implements FarePolicy {
    private final Map<VehicleClass, Rates> rates;
    StandardFare(Map<VehicleClass, Rates> rates) { this.rates = rates; }

    public Money estimate(TripEstimate t, double surge) {
        Rates r = rates.get(t.vehicle());
        Money raw = r.base().plus(r.perKm().times(t.km())).plus(r.perMinute().times(t.duration().toMinutes()));
        return raw.times(surge).max(r.minimumFare());
    }
}

/** Surge per area cell = demand / supply over the last few minutes, clamped. */
final class SurgeCalculator {
    double multiplier(int openRequests, int availableDrivers) {
        if (availableDrivers == 0) return 2.5;
        double ratio = (double) openRequests / availableDrivers;
        // smooth, capped; tuned with data in reality
        return Math.min(2.5, Math.max(1.0, 0.8 + 0.4 * ratio));
    }
}
