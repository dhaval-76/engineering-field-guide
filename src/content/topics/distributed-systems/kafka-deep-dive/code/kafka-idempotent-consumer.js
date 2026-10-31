// kafkajs + node-postgres: claim the event id and record the outcome in ONE transaction.
await consumer.run({
  autoCommit: false,
  eachMessage: async ({ topic, partition, message }) => {
    const e = JSON.parse(message.value);
    const c = await pool.connect();
    try {
      await c.query('BEGIN');
      const claimed = await c.query('INSERT INTO processed_events (event_id) VALUES ($1) ON CONFLICT DO NOTHING', [e.eventId]);
      if (claimed.rowCount === 1) {
        // The external side effect carries its own idempotency key: a retry after a crash
        // can't double-charge.
        const charge = await gateway.charge(e.customerId, e.amount, { idempotencyKey: `payment-${e.eventId}` });
        await c.query('INSERT INTO payments (event_id, charge_id, amount) VALUES ($1, $2, $3)', [e.eventId, charge.id, e.amount]);
      }                                                 // duplicate: nothing to do
      await c.query('COMMIT');
    } catch (err) {
      await c.query('ROLLBACK');
      // not committed → the message is redelivered
      throw err;
    } finally {
      c.release();
    }
    // commit the NEXT offset only after the database commit
    await consumer.commitOffsets([{ topic, partition, offset: (BigInt(message.offset) + 1n).toString() }]);
  },
});
