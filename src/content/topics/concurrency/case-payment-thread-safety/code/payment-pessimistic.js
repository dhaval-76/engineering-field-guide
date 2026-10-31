// Pessimistic: lock both rows (SELECT … FOR UPDATE) in a consistent order for the whole transaction.
export async function transfer(pool, fromId, toId, amount) {
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    const { rows } = await c.query(
      'SELECT id, balance FROM accounts WHERE id = ANY($1) ORDER BY id FOR UPDATE', [[fromId, toId]]); // order → no deadlock
    const from = rows.find((r) => r.id === fromId);
    if (BigInt(from.balance) < BigInt(amount)) throw Object.assign(new Error('insufficient funds'), { status: 422 });
    await c.query('UPDATE accounts SET balance = balance - $1 WHERE id = $2', [amount, fromId]);
    await c.query('UPDATE accounts SET balance = balance + $1 WHERE id = $2', [amount, toId]);
    await c.query(`INSERT INTO transfers (from_id, to_id, amount) VALUES ($1, $2, $3)`, [fromId, toId, amount]); // ledger row
    await c.query('COMMIT');                                 // releases both row locks
  } catch (e) {
    await c.query('ROLLBACK');
    throw e;
  } finally {
    c.release();
  }
}
