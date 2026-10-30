import pg from 'pg';
const pool = new pg.Pool({ max: 20, connectionTimeoutMillis: 3000, idleTimeoutMillis: 30000 });

// Single statements: pool.query borrows and releases for you
const { rows } = await pool.query('SELECT id, total FROM orders WHERE customer_id = $1', [customerId]);

// Transactions need ONE connection for all statements — and release() in finally
const client = await pool.connect();
try {
  await client.query('BEGIN');
  await client.query('UPDATE accounts SET balance = balance - $1 WHERE id = $2', [amt, from]);
  await client.query('UPDATE accounts SET balance = balance + $1 WHERE id = $2', [amt, to]);
  await client.query('COMMIT');
} catch (e) {
  await client.query('ROLLBACK');
  throw e;
} finally {
  client.release();
}
