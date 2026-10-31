public interface PaymentProcessor {
    PaymentMethod supports();
    PaymentResult pay(Payment payment);
}

@Component class CardProcessor implements PaymentProcessor {
    public PaymentMethod supports() { return PaymentMethod.CARD; }
    public PaymentResult pay(Payment p) { /* ... */ return PaymentResult.ok(); }
}
@Component class UpiProcessor implements PaymentProcessor {
    public PaymentMethod supports() { return PaymentMethod.UPI; }
    public PaymentResult pay(Payment p) { /* ... */ return PaymentResult.ok(); }
}

// Self-registering factory: Spring injects every PaymentProcessor bean.
@Service
public class PaymentProcessors {
    private final Map<PaymentMethod, PaymentProcessor> byMethod;

    public PaymentProcessors(List<PaymentProcessor> all) {
        this.byMethod = all.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentProcessor::supports, p -> p));
        // toUnmodifiableMap throws on duplicate keys — two beans claiming UPI fail at
        // startup, not at checkout.
    }

    public PaymentProcessor forMethod(PaymentMethod m) {
        PaymentProcessor p = byMethod.get(m);
        if (p == null) throw new UnsupportedPaymentMethod(m);
        return p;
    }
}
// Adding Wallet = one new @Component class. No existing file changes.
