// application.yml (Spring Kafka)
// spring.kafka.producer:
//   acks: all                       # wait for min.insync.replicas copies
//   properties:
// enable.idempotence: true      # default since Kafka 3.0; no duplicates or reordering on
// retry
//     linger.ms: 5                  # fill batches for a few ms
//     compression.type: lz4
//     delivery.timeout.ms: 120000   # total time to keep retrying a send
// Topic: replication.factor=3, min.insync.replicas=2

@Service
class OrderEvents {
    private final KafkaTemplate<String, OrderEvent> kafka;

    CompletableFuture<SendResult<String, OrderEvent>> publish(OrderEvent e) {
        // Key = order id → all events of one order go to one partition, in order.
        return kafka.send("order-events", e.orderId(), e)
            .whenComplete((result, ex) -> {
                // alert; outbox will retry
                if (ex != null) log.error("publish failed for {}", e.orderId(), ex);
            });
    }
}
// For guaranteed publication tied to a DB change, write to an outbox table and relay (see
// Microservices).
