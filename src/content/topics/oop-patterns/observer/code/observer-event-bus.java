public final class EventBus implements AutoCloseable {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public EventBus(ExecutorService executor) { this.executor = executor; }

    @SuppressWarnings("unchecked")
    public <E> Runnable subscribe(Class<E> type, Consumer<? super E> handler) {
        Consumer<Object> h = e -> handler.accept((E) e);
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(h);
        return () -> handlers.getOrDefault(type, List.of()).remove(h);
    }

    public void publish(Object event) {
        for (Consumer<Object> h : handlers.getOrDefault(event.getClass(), List.of())) {
            executor.execute(() -> {
                try { h.accept(event); }
                catch (RuntimeException ex) { log.error("handler failed for {}", event, ex); }
            });
        }
    }

    @Override public void close() { executor.shutdown(); }
}

// try (var bus = new EventBus(Executors.newVirtualThreadPerTaskExecutor())) {
//     Runnable unsubscribe = bus.subscribe(OrderPlaced.class, e -> email.send(e));
//     bus.publish(new OrderPlaced("o-1", "c-9", BigDecimal.TEN));
// }
