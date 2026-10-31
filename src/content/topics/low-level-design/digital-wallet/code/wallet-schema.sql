-- PostgreSQL: balances can't go negative, every transaction balances, retries are
-- idempotent.
CREATE TABLE account (
  id       text   PRIMARY KEY,
  balance  bigint NOT NULL DEFAULT 0,            -- paise
  held     bigint NOT NULL DEFAULT 0,
  -- 'user' | 'system' (system accounts may go negative)
  kind     text   NOT NULL DEFAULT 'user',
  CHECK (kind = 'system' OR balance - held >= 0)
);

CREATE TABLE txn (
  id              uuid PRIMARY KEY,
  idempotency_key text UNIQUE NOT NULL,          -- a retry hits this constraint
  kind            text NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entry (
  txn_id   uuid   NOT NULL REFERENCES txn(id),
  account  text   NOT NULL REFERENCES account(id),
  amount   bigint NOT NULL CHECK (amount <> 0),  -- + credit, − debit
  PRIMARY KEY (txn_id, account)
);

-- Transfer ana → bo 5,000 paise, in one transaction
BEGIN;
INSERT INTO txn (id, idempotency_key, kind)
-- duplicate key → error → return earlier result
VALUES ('6f1c0b4e-2f4e-4c55-9a51-0d9c7c1e2a01', 'req-123', 'TRANSFER');
-- Lock both rows in a fixed order (by id) so concurrent opposite transfers can't deadlock
SELECT id FROM account WHERE id IN ('ana', 'bo') ORDER BY id FOR UPDATE;
-- 0 rows → insufficient
UPDATE account SET balance = balance - 5000 WHERE id = 'ana' AND balance - held >= 5000;
UPDATE account SET balance = balance + 5000 WHERE id = 'bo';
INSERT INTO ledger_entry VALUES
  ('6f1c0b4e-2f4e-4c55-9a51-0d9c7c1e2a01', 'ana', -5000),
  ('6f1c0b4e-2f4e-4c55-9a51-0d9c7c1e2a01', 'bo',   5000);
COMMIT;

-- Audit: every transaction sums to zero, and cached balances match the ledger
-- expect no rows
SELECT txn_id FROM ledger_entry GROUP BY txn_id HAVING sum(amount) <> 0;
SELECT a.id FROM account a LEFT JOIN ledger_entry e ON e.account = a.id
-- expect no rows
GROUP BY a.id, a.balance HAVING a.balance <> coalesce(sum(e.amount), 0);
