// Place an order and record its event in ONE local transaction (node-postgres).
export async function placeOrder(pool, cmd) {
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    const { rows: [order] } = await c.query(
      'INSERT INTO orders (customer_id, total) VALUES ($1, $2) RETURNING id, customer_id, total', [cmd.customerId, cmd.total]);
    await c.query(
      `INSERT INTO outbox (id, aggregate_type, aggregate_id, type, payload)
       VALUES (gen_random_uuid(), 'Order', $1, 'OrderPlaced', $2)`,   // event id: consumers de-duplicate on it
      [String(order.id), { orderId: order.id, customerId: order.customer_id, total: order.total }]);
    await c.query('COMMIT');
    return order; // ✗ never producer.send() here: the commit might still fail
  } catch (e) {
    await c.query('ROLLBACK');
    throw e;
  } finally {
    c.release();
  }
}
// The relay (SELECT … FOR UPDATE SKIP LOCKED → publish → mark sent) is shown in Sagas, outbox and inbox.
// Delivery is at-least-once: consumers must be idempotent.
