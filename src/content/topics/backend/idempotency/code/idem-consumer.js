// Kafka/SQS/RabbitMQ deliver at-least-once: the same message can arrive twice (redelivery, rebalances, retries).
export async function onPaymentCaptured(pool, event) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const fresh = await client.query(
      `INSERT INTO processed_messages (consumer, message_id) VALUES ('invoicing', $1) ON CONFLICT DO NOTHING`,
      [event.eventId]);
    if (fresh.rowCount === 1) await invoices.createFor(client, event.orderId, event.amount); // duplicate → skip
    await client.query('COMMIT'); // the dedupe row and the invoice commit together — or neither does
  } catch (e) {
    await client.query('ROLLBACK');
    throw e; // not acknowledged → redelivered later
  } finally {
    client.release();
  }
}
