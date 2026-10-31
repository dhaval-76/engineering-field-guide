# Kafka/SQS/RabbitMQ deliver at-least-once: the same message can arrive twice (redelivery,
# rebalances, retries).
def on_payment_captured(pool, event) -> None:
    # dedupe row and effect commit together
    with pool.connection() as conn, conn.transaction():
        fresh = conn.execute(
            """INSERT INTO processed_messages (consumer, message_id) VALUES ('invoicing', %s)
               ON CONFLICT DO NOTHING""", (event["event_id"],)).rowcount
        # 0 → duplicate delivery: effect already applied
        if fresh == 1:
            invoices.create_for(conn, event["order_id"], event["amount"])
    # an exception rolls back both and the message is redelivered later
