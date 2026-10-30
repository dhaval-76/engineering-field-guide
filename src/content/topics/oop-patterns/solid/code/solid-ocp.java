// ✗ Every new discount type edits this method (and risks breaking the others).
double price(Order o, String type) {
    if (type.equals("FLAT"))     return o.total() - 100;
    if (type.equals("PERCENT"))  return o.total() * 0.9;
    if (type.equals("SEASONAL")) return o.total() * 0.8;
    throw new IllegalArgumentException(type);
}

// ✓ Closed for modification, open for extension: new behaviour = new class.
interface Discount { double apply(Order o); }

record FlatDiscount(double off) implements Discount     { public double apply(Order o) { return o.total() - off; } }
record PercentDiscount(double pct) implements Discount  { public double apply(Order o) { return o.total() * (1 - pct); } }
record LoyaltyDiscount(int tier) implements Discount    { public double apply(Order o) { return o.total() * (1 - 0.02 * tier); } }

class Checkout {
    double price(Order o, Discount d) { return d.apply(o); }   // never changes
}
