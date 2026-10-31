// Consumer side (Spring for Apache Kafka 2.7+): when *your* dependency (database, partner API) is down,
// don't block the partition or drop the record — move it to delayed retry topics, then a dead-letter topic.
@Component
class OrderEventsListener {

    @RetryableTopic(
        attempts = "4",                                                   // 1 try + 3 retries
        backoff = @Backoff(delay = 2_000, multiplier = 3, maxDelay = 60_000), // orders-retry-0/1/2 at 2 s, 6 s, 18 s
        exclude = { ValidationException.class, DeserializationException.class }, // bugs and bad data: straight to the DLT
        dltTopicSuffix = "-dlt")
    @KafkaListener(topics = "orders", groupId = "shipping")
    void onOrder(OrderPlaced event) {
        // Retries and replays deliver duplicates: process each event id once (inbox table, unique constraint).
        inbox.processOnce(event.id(), () -> shipments.create(event));
    }

    @DltHandler
    void onDeadLetter(OrderPlaced event, @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String error) {
        // Park it, alert a human, and offer a "replay from DLT" tool once the cause is fixed.
        deadLetters.save(event, error);
        alerts.raise("order " + event.id() + " dead-lettered: " + error);
    }
}
