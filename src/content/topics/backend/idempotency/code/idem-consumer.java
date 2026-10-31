// Kafka/SQS/RabbitMQ deliver at-least-once: the same message can arrive twice
// (redelivery, rebalances, retries).
@KafkaListener(topics = "payments.captured")
@Transactional
public void onPaymentCaptured(PaymentCaptured event) {
    int fresh = jdbc.update("""
        INSERT INTO processed_messages(consumer, message_id) VALUES ('invoicing', ?)
        ON CONFLICT DO NOTHING""", event.eventId());
    // duplicate delivery: effect already applied
    if (fresh == 0) return;

    invoices.createFor(event.orderId(), event.amount());
}   // the dedupe row and the invoice commit together — or neither does
