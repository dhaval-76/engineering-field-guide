// node-postgres. Allowed transitions as data:
const NEXT = {
  PLACED: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PREPARING', 'CANCELLED'],
  PREPARING: ['OUT_FOR_DELIVERY', 'CANCELLED'],
  OUT_FOR_DELIVERY: ['DELIVERED'],
  DELIVERED: ['RETURNED'],
  CANCELLED: [],
  RETURNED: [],
};
export const canMove = (from, to) => NEXT[from].includes(to);
export class ConcurrentStatusChange extends Error {}

// Applies a partner event exactly once and only if it's a valid next step.
export async function applyEvent(pool, orderId, eventId, next, actor) {
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    const fresh = await c.query(
      'INSERT INTO processed_events (event_id) VALUES ($1) ON CONFLICT DO NOTHING',
      [eventId],
    );
    // duplicate delivery
    if (fresh.rowCount === 0) return rollback(c, false);

    const {
      rows: [o],
    } = await c.query('SELECT status FROM orders WHERE id = $1', [orderId]);
    // late or invalid: ignore (and log)
    if (!canMove(o.status, next)) return rollback(c, false);

    // conditional update
    const moved = await c.query(
      'UPDATE orders SET status = $1, updated_at = now() WHERE id = $2 AND status = $3',
      [next, orderId, o.status],
    );
    // caller retries
    if (moved.rowCount === 0) throw new ConcurrentStatusChange(String(orderId));

    await c.query(
      'INSERT INTO order_status_history (order_id, status, actor, at) VALUES ($1, $2, $3, now())',
      [orderId, next, actor],
    );
    await c.query(
      `INSERT INTO outbox (id, type, aggregate_id, payload) VALUES (gen_random_uuid(), 'OrderStatusChanged', $1, $2)`,
      [String(orderId), { orderId, status: next }],
    );
    await c.query('COMMIT');
    return true;
  } catch (e) {
    await c.query('ROLLBACK');
    throw e;
  } finally {
    c.release();
  }
}

async function rollback(c, result) {
  // the processed_events row goes too, so a valid later delivery isn't lost
  await c.query('ROLLBACK');
  return result;
}
