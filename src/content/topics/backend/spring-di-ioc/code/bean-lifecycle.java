@Component
class ExchangeRates {
    private final RatesClient client;
    private volatile Map<Currency, BigDecimal> rates = Map.of();

    // dependencies are available here with constructor injection…
    ExchangeRates(RatesClient client) {
        this.client = client;
    }

    // …but start work after the bean is fully initialised:
    @PostConstruct
    // all injection done, before any other bean uses it
    void warmUp() { refresh(); }

    @Scheduled(fixedDelay = 60_000)
    void refresh() { rates = Map.copyOf(client.fetchAll()); }

    @PreDestroy                                  // graceful shutdown: release resources
    void close() { client.close(); }
}
// Note: @PostConstruct runs on the raw object, before AOP proxies are applied — calling a
// @Transactional method of this bean from @PostConstruct runs WITHOUT a transaction. For
// "run when the app is ready", listen for ApplicationReadyEvent instead.
