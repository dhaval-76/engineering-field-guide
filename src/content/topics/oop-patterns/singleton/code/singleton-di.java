// Preferred in applications: let the container own "one instance" and inject it.
@Configuration
class AppConfig {
    @Bean                                  // Spring beans are singleton-scoped by default
    Clock clock() { return Clock.systemUTC(); }
}

@Service
class InvoiceService {
    private final Clock clock;             // dependency is visible in the constructor
    InvoiceService(Clock clock) { this.clock = clock; }

    Invoice issue(Order o) { return new Invoice(o, clock.instant()); }
}

// In a test, pass a fixed clock — no global state to reset:
var service = new InvoiceService(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
