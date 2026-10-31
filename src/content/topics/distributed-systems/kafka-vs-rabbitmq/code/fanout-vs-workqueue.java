// ── Kafka: one event, many independent consumers (one consumer GROUP per service) ──
@Component
class InventoryListener {
    @KafkaListener(topics = "order-events", groupId = "inventory")   // gets every event
    void on(OrderPlaced e) { inventory.reserve(e.orderId(), e.lines()); }
}

@Component
class EmailListener {
    @KafkaListener(topics = "order-events", groupId = "email")       // also gets every event
    void on(OrderPlaced e) { emails.sendConfirmation(e); }
}
// Analytics added two weeks later: a new group "analytics" with auto-offset-reset=earliest
// reads the whole retained history; the other groups are unaffected.
//   kafka-consumer-groups.sh --group analytics --topic order-events --reset-offsets --to-earliest --execute

// ── RabbitMQ: a work queue — each job goes to ONE worker ──────────────────────────
@Configuration
class ThumbnailQueueConfig {
    @Bean Queue thumbnails() {
        return QueueBuilder.durable("thumbnails")
            .quorum()                                          // replicated (Raft) queue
            .deadLetterExchange("dlx").deadLetterRoutingKey("thumbnails.failed")
            .deliveryLimit(5)                                  // poison message → DLQ after 5 tries
            .build();
    }
}

@Component
class ThumbnailWorker {
    // Run 10 instances: RabbitMQ spreads jobs across them; prefetch limits in-flight work per worker.
    @RabbitListener(queues = "thumbnails", concurrency = "4")
    void handle(ThumbnailJob job) {
        images.resize(job.imageId(), 320);                     // throwing → nack → redelivered
    }
}

@Service
class Uploads {
    void uploaded(String imageId) {
        rabbit.convertAndSend("", "thumbnails", new ThumbnailJob(imageId));   // default exchange → queue
    }
}
