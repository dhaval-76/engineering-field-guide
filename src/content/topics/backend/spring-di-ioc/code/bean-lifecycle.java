@Component
class ExchangeRates {
    private final RatesClient client;
    private volatile Map<Currency, BigDecimal> rates = Map.of();

    ExchangeRates(RatesClient client) {          // dependencies are available here with constructor injection…
        this.client = client;
    }

    @PostConstruct                               // …but start work after the bean is fully initialised:
    void warmUp() { refresh(); }                 // all injection done, before any other bean uses it

    @Scheduled(fixedDelay = 60_000)
    void refresh() { rates = Map.copyOf(client.fetchAll()); }

    @PreDestroy                                  // graceful shutdown: release resources
    void close() { client.close(); }
}
// Note: @PostConstruct runs on the raw object, before AOP proxies are applied —
// calling a @Transactional method of this bean from @PostConstruct runs WITHOUT a transaction.
// For "run when the app is ready", listen for ApplicationReadyEvent instead.
