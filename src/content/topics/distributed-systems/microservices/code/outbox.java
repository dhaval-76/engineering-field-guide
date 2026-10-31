@Service
class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final ObjectMapper json;

    @Transactional                                      // ONE local transaction for both writes
    public Order place(PlaceOrder cmd) throws JsonProcessingException {
        Order order = orders.save(Order.from(cmd));
        outbox.save(new OutboxEvent(
            UUID.randomUUID(),                          // event id: consumers de-duplicate on it
            "Order", order.getId().toString(),          // aggregate type + id → Kafka key (ordering per order)
            "OrderPlaced",
            json.writeValueAsString(OrderPlaced.from(order))));
        return order;                                   // ✗ never kafka.send() here: the commit may still fail
    }
}

// Relay: publish unsent rows, mark them sent. Run on every instance — SKIP LOCKED keeps them apart.
// (Alternatively, Debezium reads the outbox table from the WAL and needs no polling.)
@Component
class OutboxRelay {
    @Scheduled(fixedDelay = 200)
    @Transactional
    public void publishBatch() {
        for (OutboxEvent e : outbox.lockNextBatch(100)) {    // SELECT … FOR UPDATE SKIP LOCKED
            kafka.send("orders", e.aggregateId(), e.payload())
                 .get(5, TimeUnit.SECONDS);                  // wait for the broker's ack (simplified)
            e.markSent(Instant.now());
        }
    }
}
// Delivery is at-least-once (a crash after send, before commit, re-sends): consumers must be idempotent.
