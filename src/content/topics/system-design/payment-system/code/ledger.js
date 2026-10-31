// node-postgres. Moves `amount` minor units between accounts exactly once per idempotency key.
export class InsufficientFunds extends Error {}

export async function transfer(pool, idempotencyKey, from, to, amount) {
  if (!(Number.isInteger(amount) && amount > 0)) throw new Error('amount must be a positive integer');
  const c = await pool.connect();
  try {
    await c.query('BEGIN');
    // 1. Claim the key. A retry (or a concurrent duplicate) inserts nothing → return the original journal.
    const claim = await c.query(
      `INSERT INTO journal (id, idempotency_key, kind) VALUES (gen_random_uuid(), $1, 'transfer')
       ON CONFLICT (idempotency_key) DO NOTHING RETURNING id`, [idempotencyKey]);
    if (claim.rowCount === 0) {
      await c.query('ROLLBACK');
      return (await pool.query('SELECT id FROM journal WHERE idempotency_key = $1', [idempotencyKey])).rows[0].id;
    }
    const journalId = claim.rows[0].id;
    // 2. Lock both accounts in a consistent order — no deadlocks between opposite transfers.
    await c.query('SELECT id FROM accounts WHERE id = ANY($1) ORDER BY id FOR UPDATE', [[from, to]]);
    // 3. Debit with a guard; 0 rows → insufficient funds → everything (including the claim) rolls back.
    const debit = await c.query(
      'UPDATE accounts SET balance = balance - $1 WHERE id = $2 AND (allow_negative OR balance >= $1)', [amount, from]);
    if (debit.rowCount === 0) throw new InsufficientFunds(`account ${from}`);
    await c.query('UPDATE accounts SET balance = balance + $1 WHERE id = $2', [amount, to]);
    // 4. The immutable record: entries sum to zero. 5. Event in the same transaction (outbox).
    await c.query('INSERT INTO entries (journal_id, account_id, amount) VALUES ($1, $2, $3), ($1, $4, $5)',
      [journalId, from, -amount, to, amount]);
    await c.query(`INSERT INTO outbox (id, type, payload) VALUES (gen_random_uuid(), 'TransferCompleted', $1)`,
      [{ journalId, amount }]);
    await c.query('COMMIT');
    return journalId;
  } catch (e) {
    await c.query('ROLLBACK').catch(() => {});
    throw e;
  } finally {
    c.release();
  }
}
