// Check-then-act across an await: two requests both see 1000 ≥ 500 and both withdraw.

// ✓ In one process: serialise per account with a promise chain (a tiny mutex)
const chains = new Map();
function withAccountLock(id, fn) {
  const prev = chains.get(id) ?? Promise.resolve();
  const next = prev.then(fn, fn);                 // run after the previous holder finishes (even if it failed)
  chains.set(id, next.catch(() => {}));
  return next;
}

export const withdraw = (account, amount) => withAccountLock(account.id, async () => {
  if (account.balance < amount) throw new Error('insufficient funds');
  await audit.record(account.id, amount);         // the await no longer lets another withdrawal in between
  account.balance -= amount;
});

// ✓ Several app servers: the database must do it atomically
// UPDATE accounts SET balance = balance - $1 WHERE id = $2 AND balance >= $1;   -- 0 rows ⇒ insufficient
