public interface PaymentProcessor { PaymentResult pay(Payment p); }

// Each decorator implements the interface AND wraps another implementation of it.
final class LoggingProcessor implements PaymentProcessor {
    private final PaymentProcessor next;
    LoggingProcessor(PaymentProcessor next) { this.next = next; }
    public PaymentResult pay(Payment p) {
        log.info("pay {} {}", p.id(), p.amount());
        PaymentResult r = next.pay(p);
        log.info("pay {} → {}", p.id(), r.status());
        return r;
    }
}

final class RetryingProcessor implements PaymentProcessor {
    private final PaymentProcessor next;
    private final int maxAttempts;
    RetryingProcessor(PaymentProcessor next, int maxAttempts) { this.next = next; this.maxAttempts = maxAttempts; }
    public PaymentResult pay(Payment p) {
        for (int attempt = 1; ; attempt++) {
            // safe only because pay() is idempotent per p.id()
            try { return next.pay(p); }
            catch (TransientGatewayException e) {
                if (attempt == maxAttempts) throw e;
                sleep(Duration.ofMillis(100L << attempt));
            }
        }
    }
}

final class MeteredProcessor implements PaymentProcessor {
    private final PaymentProcessor next;
    private final Timer timer;
    MeteredProcessor(PaymentProcessor next, MeterRegistry registry) { this.next = next; this.timer = registry.timer("payments.pay"); }
    public PaymentResult pay(Payment p) { return timer.record(() -> next.pay(p)); }
}

// Compose at startup; order matters (metrics see the total time including retries here).
PaymentProcessor processor =
    new MeteredProcessor(new LoggingProcessor(new RetryingProcessor(new CardGatewayProcessor(client), 3)), registry);
