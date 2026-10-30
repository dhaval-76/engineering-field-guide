-- Two concurrent requests, balance 1000, each debiting 900:
UPDATE accounts SET balance = balance - 900 WHERE id = 'ACC1' AND balance >= 900;
-- Request A: takes the row lock, condition true → balance 100, 1 row updated.
-- Request B: waits for A's row lock; after A commits it re-evaluates the WHERE on the
--            new row (100 >= 900 is false) → 0 rows updated → "insufficient funds".

-- PostgreSQL: RETURNING gives you the new balance in the same round trip
UPDATE accounts SET balance = balance - 900 WHERE id = 'ACC1' AND balance >= 900 RETURNING balance;

-- A CHECK constraint is a last line of defence, never the main mechanism
ALTER TABLE accounts ADD CONSTRAINT balance_non_negative CHECK (balance >= 0);
