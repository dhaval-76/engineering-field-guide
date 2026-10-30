// Without DI: the class builds its own dependencies — hard-wired, hard to test, hard to swap
class CheckoutService {
    private final PaymentGateway gateway = new StripeGateway("sk_live_…");   // ✗
}

// With DI: dependencies come in through the constructor; the container (or a test) supplies them
@Service
class CheckoutService {
    private final PaymentGateway gateway;          // an interface — Stripe, Razorpay or a fake
    private final OrderRepository orders;

    CheckoutService(PaymentGateway gateway, OrderRepository orders) {   // single constructor: no @Autowired needed
        this.gateway = gateway;
        this.orders = orders;
    }
}

// ✗ Field injection: works, but fields can't be final, dependencies are hidden,
//   and `new CheckoutService()` in a test leaves them null.
@Service
class LegacyCheckoutService {
    @Autowired private PaymentGateway gateway;
}
