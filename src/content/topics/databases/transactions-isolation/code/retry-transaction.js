// node-postgres
const RETRYABLE = new Set(['40001', '40P01']);   // serialization failure, deadlock

export async function withSerializableTx(pool, work, maxAttempts = 5) {
  for (let attempt = 1; ; attempt++) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN ISOLATION LEVEL SERIALIZABLE');
      const result = await work(client);          // all reads and writes go through this client
      await client.query('COMMIT');
      return result;
    } catch (err) {
      await client.query('ROLLBACK').catch(() => {});
      if (attempt >= maxAttempts || !RETRYABLE.has(err.code)) throw err;
      const max = Math.min(1000, 20 * 2 ** attempt);
      await new Promise((r) => setTimeout(r, Math.random() * max));   // jittered backoff
    } finally {
      client.release();
    }
  }
}

// await withSerializableTx(pool, (c) => takeOffCall(c, 'alice', 1));
