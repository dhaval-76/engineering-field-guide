// build: io.github.resilience4j:resilience4j-spring-boot3 + spring-boot-starter-aop
//
// application.yml
// resilience4j:
//   circuitbreaker.instances.inventory:
//     sliding-window-type: COUNT_BASED
//     sliding-window-size: 20
//     minimum-number-of-calls: 10           # don't judge on 2 calls
//     failure-rate-threshold: 50            # % failures that opens the circuit
//     slow-call-duration-threshold: 1s      # slow calls count as bad too…
//     slow-call-rate-threshold: 50          # …and can open it on their own
//     wait-duration-in-open-state: 20s
//     permitted-number-of-calls-in-half-open-state: 3
//     ignore-exceptions: [com.shop.inventory.ProductNotFoundException]   # 404s aren't failures
//   retry.instances.inventory:
//     max-attempts: 3                       # 1 call + 2 retries
//     wait-duration: 100ms
//     enable-exponential-backoff: true
//     exponential-backoff-multiplier: 2
//     enable-randomized-wait: true          # jitter
//     retry-exceptions: [java.io.IOException, org.springframework.web.client.ResourceAccessException]
//   bulkhead.instances.inventory:
//     max-concurrent-calls: 20              # at most 20 threads waiting on inventory
//     max-wait-duration: 0                  # full → fail immediately
// management.endpoints.web.exposure.include: health,circuitbreakers,retries

@Service
class InventoryGateway {
    private final InventoryClient client;    // HTTP client with connect 300 ms / read 800 ms timeouts
    private final StockCache cache;

    InventoryGateway(InventoryClient client, StockCache cache) { this.client = client; this.cache = cache; }

    // Order applied: Retry( CircuitBreaker( Bulkhead( call ) ) ). The HTTP read timeout is the
    // time limit; no TimeLimiter/CompletableFuture needed for a blocking client.
    @Retry(name = "inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "lastKnown")
    @Bulkhead(name = "inventory")
    public Stock stockFor(String sku) {
        Stock s = client.stock(sku);
        cache.remember(sku, s);              // keep a last-known value for the fallback
        return s;
    }

    // Same parameters + the exception. Runs when the call fails, the circuit is open,
    // or the bulkhead is full.
    Stock lastKnown(String sku, Throwable cause) {
        return cache.lastKnown(sku)
            .map(Stock::markedStale)         // "in stock (as of 10:02)" — callers can show a hint
            .orElseThrow(() -> new InventoryUnavailableException(sku, cause));
    }
}

// Test the breaker's behaviour without a network: drive the registry directly.
@Test
void opensAfterFailureRateIsExceeded() {
    CircuitBreaker cb = CircuitBreaker.of("t", CircuitBreakerConfig.custom()
        .slidingWindowSize(10).minimumNumberOfCalls(10).failureRateThreshold(50).build());
    for (int i = 0; i < 10; i++) {
        try { cb.executeSupplier(() -> { throw new IllegalStateException("down"); }); }
        catch (IllegalStateException ignored) { }
    }
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> cb.executeSupplier(() -> "x")).isInstanceOf(CallNotPermittedException.class);
}
