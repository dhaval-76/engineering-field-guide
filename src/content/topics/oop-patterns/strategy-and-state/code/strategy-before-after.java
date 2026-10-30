// ✗ Every new shipping option edits this method; every branch is tested together.
BigDecimal shippingCost(Order o, String option) {
    if (option.equals("STANDARD")) return o.weightKg().multiply(new BigDecimal("40"));
    if (option.equals("EXPRESS"))  return o.weightKg().multiply(new BigDecimal("90")).add(new BigDecimal("50"));
    if (option.equals("PICKUP"))   return BigDecimal.ZERO;
    throw new IllegalArgumentException(option);
}

// ✓ Strategy: each algorithm in its own class behind one interface.
interface ShippingPolicy { BigDecimal cost(Order o); }

final class StandardShipping implements ShippingPolicy {
    public BigDecimal cost(Order o) { return o.weightKg().multiply(new BigDecimal("40")); }
}
final class ExpressShipping implements ShippingPolicy {
    public BigDecimal cost(Order o) { return o.weightKg().multiply(new BigDecimal("90")).add(new BigDecimal("50")); }
}
final class StorePickup implements ShippingPolicy {
    public BigDecimal cost(Order o) { return BigDecimal.ZERO; }
}

final class Checkout {                                  // the context
    private final ShippingPolicy shipping;
    Checkout(ShippingPolicy shipping) { this.shipping = shipping; }
    BigDecimal total(Order o) { return o.subtotal().add(shipping.cost(o)); }
}
