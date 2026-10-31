final class CheckoutService {
    private final CartRepository carts;
    private final PricingEngine pricing;
    private final PromotionRepository promotions;
    private final InventoryService inventory;
    private final PaymentGateway payments;
    private final OrderRepository orders;

    CheckoutService(CartRepository c, PricingEngine p, PromotionRepository pr, InventoryService i, PaymentGateway g, OrderRepository o) {
        carts = c; pricing = p; promotions = pr; inventory = i; payments = g; orders = o;
    }

    /**
     * `checkoutId` comes from the client and makes the whole operation idempotent
     * (double-clicks, retries).
     */
    Order checkout(String checkoutId, String cartId, PaymentMethod method) {
        Optional<Order> existing = orders.findByCheckoutId(checkoutId);
        if (existing.isPresent()) return existing.get();

        Cart cart = carts.get(cartId);
        // re-price: never trust client prices
        PriceBreakdown price = pricing.price(cart, promotions.activeFor(cart));

        // all-or-nothing, conditional updates
        Reservation hold = inventory.reserveAll(cart.lines(), Duration.ofMinutes(15));
        try {
            PaymentResult paid = payments.charge(price.payablePaise(), method, checkoutId);
            if (!paid.succeeded()) throw new PaymentDeclined(paid.reason());
            Order order = orders.save(Order.from(checkoutId, cart, price, paid.reference()));
            inventory.commit(hold);
            carts.clear(cartId);
            return order;
        } catch (RuntimeException e) {
            inventory.release(hold);
            throw e;
        }
    }
}

// InventoryService.reserveAll, in SQL, for each line (inside one transaction, SKUs in
// sorted order):
//   UPDATE stock SET available = available - :qty, reserved = reserved + :qty
// WHERE sku = :sku AND available >= :qty;          -- 0 rows → out of stock → roll back
// everything
