// Spring Boot 3 + Micrometer: one Observation produces a timer metric AND a trace span.
@Service
class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final ObservationRegistry observations;
    private final MeterRegistry meters;

    CheckoutService(ObservationRegistry observations, MeterRegistry meters) {
        this.observations = observations; this.meters = meters;
    }

    Order checkout(Cart cart, String channel) {
        return Observation.createNotStarted("checkout", observations)
                .lowCardinalityKeyValue("channel", channel)               // few values: OK as a label
                .highCardinalityKeyValue("cart.id", cart.id())             // span attribute only, never a metric label
                .observe(() -> {
                    Order order = placeOrder(cart);
                    meters.counter("orders.placed", "channel", channel).increment();
                    log.info("order placed orderId={} items={} totalCents={}",  // trace id added by the log pattern
                            order.id(), cart.items().size(), order.totalCents());
                    return order;
                });
    }
}

// application.properties
// management.tracing.sampling.probability=0.1
// management.otlp.tracing.endpoint=http://otel-collector:4318/v1/traces
// logging.structured.format.console=ecs                 # JSON logs with trace/span ids
// management.metrics.distribution.percentiles-histogram.http.server.requests=true
