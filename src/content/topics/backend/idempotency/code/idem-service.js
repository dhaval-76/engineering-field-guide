// node-postgres. Runs `action` at most once per (client, key); repeats get the stored
// response.
export class IdempotencyKeyReused extends Error {} // → 422
export class RequestInProgress extends Error {}    // → 409, retry later
// a final answer (402 declined…) — stored like a success
export class BusinessRejection extends Error {
  constructor(code, body) { super(body.message); Object.assign(this, { code, body }); }
}

const SAVE = `UPDATE idempotency_keys SET status = 'DONE', response_code = $1, response_body = $2
              WHERE client_id = $3 AND key = $4`;

export async function executeOnce(pool, clientId, key, requestHash, action) {
  // 1. Claim the key atomically: the primary key makes concurrent duplicates lose this
  // race.
  const claim = await pool.query(
    `INSERT INTO idempotency_keys (client_id, key, request_hash, status, locked_until)
     VALUES ($1, $2, $3, 'IN_PROGRESS', now() + interval '30 seconds')
     ON CONFLICT (client_id, key) DO NOTHING`, [clientId, key, requestHash]);

  if (claim.rowCount === 0) {
    const { rows: [e] } = await pool.query(
      'SELECT request_hash, status, response_code, response_body, locked_until > now() AS locked FROM idempotency_keys WHERE client_id = $1 AND key = $2',
      [clientId, key]);
    if (e.request_hash !== requestHash) throw new IdempotencyKeyReused();
    // replay
    if (e.status === 'DONE') return { code: e.response_code, body: e.response_body };
    if (e.locked) throw new RequestInProgress();
    // stale: take it over
    const taken = await pool.query(
      `UPDATE idempotency_keys SET locked_until = now() + interval '30 seconds'
       WHERE client_id = $1 AND key = $2 AND status = 'IN_PROGRESS' AND locked_until <= now()`, [clientId, key]);
    if (taken.rowCount === 0) throw new RequestInProgress();
  }

  // 2. Do the work and record the outcome in ONE transaction, so they can't disagree.
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    // { code, body }, using the same transaction
    const result = await action(client);
    await client.query(SAVE, [result.code, result.body, clientId, key]);
    await client.query('COMMIT');
    return result;
  } catch (e) {
    await client.query('ROLLBACK');
    // final answer: store it for retries
    if (e instanceof BusinessRejection) {
      await pool.query(SAVE, [e.code, e.body, clientId, key]);
      return { code: e.code, body: e.body };
    }
    await pool.query(`DELETE FROM idempotency_keys WHERE client_id = $1 AND key = $2 AND status = 'IN_PROGRESS'`,
      // unexpected failure: let a retry run again
      [clientId, key]);
    throw e;
  } finally {
    client.release();
  }
}
