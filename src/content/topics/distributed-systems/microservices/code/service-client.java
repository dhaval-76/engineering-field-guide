// Spring Framework 6+: declare the remote API as an interface (replaces
// Feign/RestTemplate).
@HttpExchange("/api/payments")
interface PaymentsClient {
    @PostExchange
    PaymentResult charge(@RequestBody ChargeRequest request,
                         @RequestHeader("Idempotency-Key") String idempotencyKey);

    @GetExchange("/{id}")
    PaymentResult get(@PathVariable String id);
}

@Configuration
class ClientsConfig {
    @Bean
    PaymentsClient paymentsClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        // fail fast if the host is unreachable
        factory.setConnectTimeout(Duration.ofMillis(300));
        // never wait forever for an answer
        factory.setReadTimeout(Duration.ofSeconds(2));
        // Boot's builder adds tracing headers
        RestClient rest = builder
            .baseUrl("http://payments")                     // Kubernetes Service DNS name
            .requestFactory(factory)
            .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest))
            .build()
            .createClient(PaymentsClient.class);
    }
}

// Usage — wrap with a circuit breaker and handle failure explicitly (see Resilience
// patterns): PaymentResult r = payments.charge(new ChargeRequest(orderId, amount),
// "order-" + orderId);
