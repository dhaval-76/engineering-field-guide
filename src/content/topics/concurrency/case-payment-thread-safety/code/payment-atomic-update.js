// node-postgres: the same single-statement check-and-debit
async function debit(pool, accountId, amount) {
  const { rowCount } = await pool.query(
    'UPDATE accounts SET balance = balance - $1 WHERE id = $2 AND balance >= $1',
    [amount, accountId],          // pass money as a string/decimal, never a float
  );
  return rowCount === 1;
}
