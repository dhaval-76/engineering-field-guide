// Express: the client sends an Idempotency-Key per payment attempt; retries with the same key replay the result.
app.post('/payments', async (req, res) => {
  const key = req.get('Idempotency-Key');
  if (!key) return res.status(400).json({ error: 'Idempotency-Key header required' });
  res.json(await payOnce(key, req.body));
});

async function payOnce(key, request) {
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    // exactly one request with this key gets to proceed
    const first = await c.query(
      `INSERT INTO idempotency_keys (key, request_hash, status) VALUES ($1, $2, 'IN_PROGRESS') ON CONFLICT (key) DO NOTHING`,
      [key, hash(request)]);
    if (first.rowCount === 0) {
      await c.query('ROLLBACK');
      return previousResultOrConflict(key, request); // same key: replay (or 409 if in progress / different body)
    }
    const result = await debitAndRecord(c, request); // atomic UPDATE + ledger row, same transaction
    await c.query(`UPDATE idempotency_keys SET status = 'DONE', response = $1 WHERE key = $2`, [result, key]);
    await c.query('COMMIT');
    return result;
  } catch (e) {
    await c.query('ROLLBACK');
    throw e;
  } finally {
    c.release();
  }
}
