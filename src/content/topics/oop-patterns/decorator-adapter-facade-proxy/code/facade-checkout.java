// Many subsystems, each with its own API and failure modes…
final class CheckoutFacade {
    private final CartService carts;
    private final PricingService pricing;
    private final InventoryService inventory;
    private final PaymentProcessor payments;
    private final OrderRepository orders;

    CheckoutFacade(CartService c, PricingService pr, InventoryService i, PaymentProcessor pa, OrderRepository o) {
        carts = c; pricing = pr; inventory = i; payments = pa; orders = o;
    }

    /** …one simple entry point for controllers and other callers. */
    public OrderConfirmation checkout(String cartId, PaymentDetails details) {
        Cart cart = carts.load(cartId);
        Quote quote = pricing.quote(cart);
        Reservation hold = inventory.reserve(cart.items());
        try {
            PaymentResult paid = payments.pay(Payment.of(quote.total(), details));
            if (!paid.succeeded()) throw new PaymentDeclined(paid.reason());
            Order order = orders.save(Order.from(cart, quote, paid));
            inventory.commit(hold);
            return new OrderConfirmation(order.id(), quote.total());
        } catch (RuntimeException e) {
            inventory.release(hold);                         // the facade owns the clean-up rules
            throw e;
        }
    }
}
