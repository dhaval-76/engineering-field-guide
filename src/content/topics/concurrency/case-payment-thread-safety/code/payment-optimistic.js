// Optimistic: read with a version, write only if the version is unchanged; retry on conflict.
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const httpError = (message, status) => Object.assign(new Error(message), { status });

export async function debit(pool, accountId, amount, attempts = 3) {
  for (let i = 1; i <= attempts; i++) {
    const { rows } = await pool.query('SELECT balance, version FROM accounts WHERE id = $1', [
      accountId,
    ]);
    const acc = rows[0];
    if (BigInt(acc.balance) < BigInt(amount)) throw httpError('insufficient funds', 422);
    const { rowCount } = await pool.query(
      `UPDATE accounts SET balance = balance - $1, version = version + 1
       WHERE id = $2 AND version = $3`,
      [amount, accountId, acc.version],
    );
    if (rowCount === 1) return; // nobody changed the row since we read it
    await sleep(Math.random() * 20 * i); // lost the race: back off with jitter, re-read
  }
  throw httpError('too much contention, try again', 409);
}
