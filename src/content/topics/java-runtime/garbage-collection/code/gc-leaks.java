// 1. Static collection that only grows — reachable forever, never collected
class AuditTrail {
    static final List<Event> EVENTS = new ArrayList<>();           // ✗ unbounded
    static void record(Event e) { EVENTS.add(e); }
}

// 2. Cache without a bound or expiry
Map<String, Report> cache = new ConcurrentHashMap<>();            // ✗ grows with every distinct key
Cache<String, Report> bounded = Caffeine.newBuilder()               // ✓ size + time bounds
        .maximumSize(10_000).expireAfterAccess(Duration.ofMinutes(30)).build();

// 3. ThreadLocal in a thread pool — pool threads live forever, so do their values
static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();
void handle(Request r) {
    CTX.set(new RequestContext(r));
    try { process(r); }
    finally { CTX.remove(); }                                       // ✓ always remove
}

// 4. Listeners never unregistered — the publisher keeps every subscriber alive
eventBus.subscribe(OrderPlaced.class, this::onOrder);               // ✓ keep the handle and unsubscribe on close

// 5. A non-static inner class handed to a long-lived owner keeps its outer instance alive
class Screen {
    class Refresher implements Runnable { public void run() { /* uses Screen.this implicitly */ } }
    void start() { scheduler.scheduleAtFixedRate(new Refresher(), 0, 1, SECONDS); }   // ✗ Screen can never be collected
}

// WeakHashMap is NOT a cache: entries vanish when the KEY is unreachable, not when memory is tight,
// and keys like Strings/ids are often strongly held elsewhere (or interned) — so it can leak or evict at random.
