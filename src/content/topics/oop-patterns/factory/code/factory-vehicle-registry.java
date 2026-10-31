final class VehicleFactory {
    private static final Map<VehicleType, Function<String, Vehicle>> CREATORS = new EnumMap<>(Map.of(
        VehicleType.CAR,   Car::new,
        VehicleType.BIKE,  Bike::new,
        VehicleType.TRUCK, Truck::new
    ));

    static Vehicle create(VehicleType type, String plate) {
        var creator = CREATORS.get(type);
        if (creator == null) throw new IllegalArgumentException("No creator for " + type);
        return creator.apply(plate);
    }
}
// Adding SCOOTER: one enum constant + one map entry (or a self-registering @Component in
// Spring).
