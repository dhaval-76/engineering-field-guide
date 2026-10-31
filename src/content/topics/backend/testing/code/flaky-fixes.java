// 1. Time: production code asks an injected Clock, never Instant.now() directly.
@Service
class TrialService {
    private final Clock clock;
    // a Clock bean: Clock.systemUTC()
    TrialService(Clock clock) { this.clock = clock; }

    boolean isExpired(Trial t) { return Instant.now(clock).isAfter(t.endsAt()); }
}

@Test
void trialExpiresAfterEndTime() {
    var clock = Clock.fixed(Instant.parse("2026-03-15T00:00:01Z"), ZoneOffset.UTC);
    var trial = new Trial(Instant.parse("2026-03-15T00:00:00Z"));
    // same result every run
    assertThat(new TrialService(clock).isExpired(trial)).isTrue();
}

// 2. Async: wait for the condition, not for an arbitrary number of milliseconds.
@Test
void orderPlaced_eventuallySendsConfirmationEmail() {
    service.placeOrder(request);

    await().atMost(Duration.ofSeconds(5))                  // Awaitility
           .untilAsserted(() -> assertThat(mailbox.sentTo("c1@example.com")).hasSize(1));
    // ✗ Thread.sleep(500) — too short on a busy CI machine, wasted time everywhere else
}

// 3. Shared state: each test creates the data it needs, with unique keys.
@Test
void customerCanHaveOnlyOneActiveCart() {
    // no clash with other tests
    String email = "user-" + UUID.randomUUID() + "@example.com";
    var customer = customers.create(email);
    carts.open(customer);
    assertThrows(ActiveCartExistsException.class, () -> carts.open(customer));
}
