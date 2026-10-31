// logback-spring.xml uses a JSON encoder (e.g. logstash-logback-encoder or Spring Boot's
// structured logging:
//   logging.structured.format.console: ecs). Micrometer Tracing puts traceId/spanId into the MDC,
// so EVERY log line carries them and can be joined with the trace.

@Service
class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final MeterRegistry meters;
    private final Timer checkoutTimer;

    CheckoutService(MeterRegistry meters) {
        this.meters = meters;
        this.checkoutTimer = Timer.builder("checkout.duration")
            // histograms → p99 across instances
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(500))
            .register(meters);
    }

    Receipt checkout(Cart cart) {
        return checkoutTimer.record(() -> {
            try {
                Receipt r = doCheckout(cart);
                meters.counter("checkout.result", "outcome", "ok", "payment_method", cart.paymentMethod()).increment();
                return r;
            } catch (PaymentDeclinedException e) {
                // Labels must be low-cardinality: outcome and method, never cartId or
                // userId.
                meters.counter("checkout.result", "outcome", "declined", "payment_method", cart.paymentMethod()).increment();
                log.atInfo().setMessage("payment declined")
                   // high-cardinality detail belongs in logs
                   .addKeyValue("cartId", cart.id())
                   .addKeyValue("reason", e.code())
                   .log();
                throw e;
            }
        });
    }
}
// Output (one JSON object per line):
// {"@timestamp":"…","level":"INFO","service":"checkout","traceId":"4bf92f…","spanId":"00f067…",
// "message":"payment declined","cartId":"c_881","reason":"insufficient_funds"}
