interface Beverage { String description(); int pricePaise(); }

record Espresso() implements Beverage {
    public String description() { return "Espresso"; }
    public int pricePaise() { return 12_000; }
}

// Add-ons wrap any Beverage — combinations happen at runtime, not as subclasses.
record WithMilk(Beverage inner) implements Beverage {
    public String description() { return inner.description() + ", milk"; }
    public int pricePaise() { return inner.pricePaise() + 2_000; }
}
record WithSyrup(Beverage inner, String flavour) implements Beverage {
    public String description() { return inner.description() + ", " + flavour + " syrup"; }
    public int pricePaise() { return inner.pricePaise() + 3_000; }
}

Beverage order = new WithSyrup(new WithMilk(new Espresso()), "hazelnut");
// "Espresso, milk, hazelnut syrup" — 170.00 Without decorators: EspressoWithMilk,
// EspressoWithMilkAndSyrup, … one class per combination.
