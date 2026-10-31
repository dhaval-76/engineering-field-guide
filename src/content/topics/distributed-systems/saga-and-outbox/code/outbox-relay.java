// Polling relay. Run 2+ instances; SKIP LOCKED spreads batches and survives one dying.
@Scheduled(fixedDelay = 200)
void relayOnce() {
    tx.executeWithoutResult(status -> {
        List<OutboxRow> batch = jdbc.query("""
                SELECT id, aggregate_id, event_type, payload::text FROM outbox
                WHERE published_at IS NULL ORDER BY id LIMIT 100
                FOR UPDATE SKIP LOCKED""",
                (rs, i) -> new OutboxRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        if (batch.isEmpty()) return;

        List<CompletableFuture<?>> sends = batch.stream()
                .map(r -> kafka.send(new ProducerRecord<>("orders", r.aggregateId(), r.payload()))) // key = order id
                .<CompletableFuture<?>>map(f -> f)
                .toList();
        CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).join();  // wait for broker acks

        jdbc.update("UPDATE outbox SET published_at = now() WHERE id = ANY(?)",
                (Object) batch.stream().map(OutboxRow::id).toArray(Long[]::new));
    });
    // A crash after send but before commit republishes the batch → consumers must be idempotent.
}

// Consumer with an inbox: duplicate deliveries are ignored atomically with the effect.
@KafkaListener(topics = "orders", groupId = "shipping")
@Transactional
public void onOrderEvent(ConsumerRecord<String, String> rec) {
    String eventId = new String(rec.headers().lastHeader("event-id").value(), StandardCharsets.UTF_8);
    int inserted = jdbc.update(
            "INSERT INTO inbox (consumer, event_id) VALUES ('shipping', ?) ON CONFLICT DO NOTHING", eventId);
    if (inserted == 0) return;                       // already handled
    shipments.createFor(rec.key(), rec.value());     // same DB transaction as the inbox row
}
