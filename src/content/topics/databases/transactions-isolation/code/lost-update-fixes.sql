-- ✗ The bug: read into the app, compute, write back (Read Committed).
--   T1: SELECT balance FROM accounts WHERE id = 1;          → 1000
--   T2: SELECT balance FROM accounts WHERE id = 1;          → 1000
--   T1: UPDATE accounts SET balance = 100 WHERE id = 1;     -- 1000 - 900
--   T2: UPDATE accounts SET balance = 100 WHERE id = 1;     -- waits for T1, then overwrites
--   Two debits of 900, balance 100. One debit is lost.

-- ✓ 1. Atomic update: the database reads and writes under the row lock.
UPDATE accounts SET balance = balance - 900
WHERE id = 1 AND balance >= 900;                -- 0 rows → insufficient funds

-- ✓ 2. Pessimistic: lock the row while deciding.
BEGIN;
SELECT balance FROM accounts WHERE id = 1 FOR UPDATE;   -- T2 waits here until T1 commits
-- … application logic: limits, fees, fraud checks …
UPDATE accounts SET balance = balance - 900 WHERE id = 1;
COMMIT;

-- ✓ 3. Optimistic: write only if nobody changed it since we read it.
UPDATE accounts SET balance = 100, version = version + 1
WHERE id = 1 AND version = 7;                   -- 0 rows → reload and retry, or 409

-- ✓ 4. Let the database detect it: Repeatable Read / Serializable in PostgreSQL.
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM accounts WHERE id = 1;      -- 1000
UPDATE accounts SET balance = 100 WHERE id = 1;
COMMIT;   -- the second of two such transactions gets:
          -- ERROR: could not serialize access due to concurrent update  (SQLSTATE 40001) → retry
