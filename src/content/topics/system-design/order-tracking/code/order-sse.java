@RestController
class TrackingStream {
    private final OrderReads orders;
    private final ReactiveRedisMessageListenerContainer redis; // pub/sub

    @GetMapping(value = "/orders/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<StatusView>> stream(
            @PathVariable long id,
            @RequestHeader(value = "Last-Event-ID", required = false) Long lastSeen,
            Principal user) {
        orders.requireOwner(id, user);

        // 1. Catch-up: history after the last event the browser saw (or the full
        // timeline).
        Flux<StatusView> history =
                Flux.fromIterable(orders.historyAfter(id, lastSeen == null ? 0 : lastSeen));

        // 2. Live: changes published for this order while the stream is open.
        Flux<StatusView> live =
                redis.receive(ChannelTopic.of("order:" + id))
                        .map(msg -> StatusView.parse(msg.getMessage()));

        return Flux.concat(history, live)
                .map(
                        s ->
                                ServerSentEvent.builder(s)
                                        .id(String.valueOf(s.sequence()))
                                        .event("status")
                                        .build())
                // keep proxies from closing idle streams
                .mergeWith(
                        Flux.interval(Duration.ofSeconds(25))
                                .map(
                                        i ->
                                                ServerSentEvent.<StatusView>builder()
                                                        .comment("ping")
                                                        .build()));
    }
}
