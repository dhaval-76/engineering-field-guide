// Spring MVC: one SSE stream per order; replays missed events using Last-Event-ID.
@GetMapping(path = "/orders/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
SseEmitter orderEvents(@PathVariable String id,
                       @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
                       @AuthenticationPrincipal User user) {
    orders.checkCanRead(user, id);                                   // authorize once, at connect
    SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());

    Consumer<OrderEvent> send = e -> {
        try {
            emitter.send(SseEmitter.event().id(e.id()).name("status").data(e));
        } catch (IOException ex) {
            emitter.completeWithError(ex);                           // client went away
        }
    };
    eventStore.eventsAfter(id, lastEventId).forEach(send);          // catch up after a reconnect
    Subscription sub = eventBus.subscribe("order:" + id, send);      // live events from pub/sub
    ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
        try { emitter.send(SseEmitter.event().comment("ping")); } catch (IOException ignored) { }
    }, 15, 15, TimeUnit.SECONDS);                                    // keeps proxies from closing idle streams

    Runnable cleanup = () -> { sub.cancel(); heartbeat.cancel(false); };
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(t -> cleanup.run());
    return emitter;
}
