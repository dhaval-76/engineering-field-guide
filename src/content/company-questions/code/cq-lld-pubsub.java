record Event(String type, String payload) {}

interface Subscriber {
    void onEvent(Event e);
}

/**
 * Each subscriber gets its own single-thread queue: a slow or failing subscriber can't delay the
 * others, and each one still sees events in the order they were published.
 */
class EventBus implements AutoCloseable {
    private final Map<String, Set<Subscriber>> byType = new ConcurrentHashMap<>();
    private final Map<Subscriber, ExecutorService> mailboxes = new ConcurrentHashMap<>();
    final AtomicLong failures = new AtomicLong();

    void subscribe(String type, Subscriber s) {
        byType.computeIfAbsent(type, t -> new CopyOnWriteArraySet<>()).add(s);
        mailboxes.computeIfAbsent(s, x -> Executors.newSingleThreadExecutor());
    }

    void unsubscribe(String type, Subscriber s) {
        Set<Subscriber> subs = byType.get(type);
        if (subs != null) subs.remove(s);
    }

    void publish(Event e) {
        for (Subscriber s : byType.getOrDefault(e.type(), Set.of())) {
            mailboxes
                    .get(s)
                    .execute(
                            () -> {
                                try {
                                    s.onEvent(e);
                                } catch (
                                        RuntimeException
                                                ex) { // isolate failures; retry or dead-letter here
                                    failures.incrementAndGet();
                                }
                            });
        }
    }

    public void close() throws InterruptedException {
        for (ExecutorService m : mailboxes.values()) {
            m.shutdown();
            m.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
