@Service
class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final ObjectMapper json;

    // ONE local transaction for both writes
    @Transactional
    public Order place(PlaceOrder cmd) throws JsonProcessingException {
        Order order = orders.save(Order.from(cmd));
        outbox.save(new OutboxEvent(
            // event id: consumers de-duplicate on it
            UUID.randomUUID(),
            // aggregate type + id → Kafka key (ordering per order)
            "Order", order.getId().toString(),
            "OrderPlaced",
            json.writeValueAsString(OrderPlaced.from(order))));
        // ✗ never kafka.send() here: the commit may still fail
        return order;
    }
}

// Relay: publish unsent rows, mark them sent. Run on every instance — SKIP LOCKED keeps
// them apart. (Alternatively, Debezium reads the outbox table from the WAL and needs no
// polling.)
@Component
class OutboxRelay {
    @Scheduled(fixedDelay = 200)
    @Transactional
    public void publishBatch() {
        // SELECT … FOR UPDATE SKIP LOCKED
        for (OutboxEvent e : outbox.lockNextBatch(100)) {
            kafka.send("orders", e.aggregateId(), e.payload())
                 // wait for the broker's ack (simplified)
                 .get(5, TimeUnit.SECONDS);
            e.markSent(Instant.now());
        }
    }
}
// Delivery is at-least-once (a crash after send, before commit, re-sends): consumers must
// be idempotent.
