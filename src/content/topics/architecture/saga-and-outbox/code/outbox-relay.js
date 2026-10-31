// Polling relay with node-postgres + kafkajs. Several instances can run: SKIP LOCKED splits the work.
export async function relayOnce(pool, producer) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const { rows } = await client.query(
      `SELECT id, aggregate_id, payload::text AS payload FROM outbox
       WHERE published_at IS NULL ORDER BY id LIMIT 100
       FOR UPDATE SKIP LOCKED`,
    );
    if (rows.length) {
      await producer.send({
        topic: 'orders',
        acks: -1, // wait for all in-sync replicas
        messages: rows.map((r) => ({ key: r.aggregate_id, value: r.payload, headers: { 'event-id': String(r.id) } })),
      });
      await client.query('UPDATE outbox SET published_at = now() WHERE id = ANY($1)', [rows.map((r) => r.id)]);
    }
    await client.query('COMMIT');
    return rows.length;
  } catch (e) {
    await client.query('ROLLBACK'); // rows unlock and are retried; duplicates are possible → idempotent consumers
    throw e;
  } finally {
    client.release();
  }
}
