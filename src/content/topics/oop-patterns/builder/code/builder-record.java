// Java records give immutable value objects with a compact constructor for validation.
// Pair a record with a small builder when there are many optional fields.
public record Pizza(Size size, Crust crust, List<String> toppings, boolean extraCheese) {
    public Pizza {
        Objects.requireNonNull(size, "size");
        // defensive, immutable copy
        toppings = List.copyOf(toppings);
        if (crust == Crust.STUFFED && size != Size.LARGE)
            throw new IllegalArgumentException("stuffed crust is only available in LARGE");
    }

    public static Builder builder(Size size) { return new Builder(size); }

    public static final class Builder {
        private final Size size;
        private Crust crust = Crust.REGULAR;
        private final List<String> toppings = new ArrayList<>();
        private boolean extraCheese;
        private Builder(Size size) { this.size = size; }
        public Builder crust(Crust c)       { crust = c; return this; }
        public Builder topping(String t)    { toppings.add(t); return this; }
        public Builder extraCheese()        { extraCheese = true; return this; }
        public Pizza build()                { return new Pizza(size, crust, toppings, extraCheese); }
    }
}
