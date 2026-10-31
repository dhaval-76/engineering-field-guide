// ✗ BROKEN — check-then-act across an await, and money as a floating-point number
async function debit(accountId, amount) {
  // request A and request B both read 1000
  const account = await db.findAccount(accountId);
  if (account.balance >= amount) {
    account.balance -= amount;                       // both compute 100
    await db.saveAccount(account);                   // last write wins: one debit is lost
  }
}
// 0.1 + 0.2 === 0.30000000000000004 — store integer minor units (paise/cents) instead.
// An in-process mutex wouldn't help: several Node processes or servers each load their
// own copy.
