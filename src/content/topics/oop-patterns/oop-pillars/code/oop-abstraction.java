// The contract: WHAT a payment processor does. Callers only ever see this.
public interface PaymentProcessor {
    PaymentResult charge(Money amount, String customerId);
    PaymentResult refund(String transactionId, Money amount);
}

// One HOW. Retries, HTTP calls and API keys stay hidden in here.
class CardProcessor implements PaymentProcessor {
    public PaymentResult charge(Money amount, String customerId) { /* call card gateway */ return PaymentResult.ok(); }
    public PaymentResult refund(String txnId, Money amount)      { /* ... */ return PaymentResult.ok(); }
}

class UpiProcessor implements PaymentProcessor {
    public PaymentResult charge(Money amount, String customerId) { /* call UPI switch */ return PaymentResult.ok(); }
    public PaymentResult refund(String txnId, Money amount)      { /* ... */ return PaymentResult.ok(); }
}

class CheckoutService {
    private final PaymentProcessor payments;            // typed by the abstraction

    CheckoutService(PaymentProcessor payments) { this.payments = payments; }

    void checkout(Order order) {
        payments.charge(order.total(), order.customerId()); // works for any processor
    }
}
