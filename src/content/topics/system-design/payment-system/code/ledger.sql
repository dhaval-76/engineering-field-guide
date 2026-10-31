CREATE TABLE accounts (
    id        bigint PRIMARY KEY,
    -- 'wallet:u42', 'merchant:42', 'psp_clearing', 'fees_revenue'
    owner     text NOT NULL,
    currency  char(3) NOT NULL,
    -- minor units; kept in step with entries in the same transaction
    balance   bigint NOT NULL DEFAULT 0,
    allow_negative boolean NOT NULL DEFAULT false,
    CHECK (allow_negative OR balance >= 0)
);

CREATE TABLE journal (                     -- one row per business transaction
    id              uuid PRIMARY KEY,
    -- a retried request can't create a second journal
    idempotency_key text NOT NULL UNIQUE,
    -- 'transfer', 'capture', 'refund', 'payout', 'reversal'
    kind            text NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE entries (                     -- append-only: never UPDATE or DELETE
    journal_id  uuid NOT NULL REFERENCES journal(id),
    account_id  bigint NOT NULL REFERENCES accounts(id),
    -- positive = credit, negative = debit
    amount      bigint NOT NULL CHECK (amount <> 0),
    PRIMARY KEY (journal_id, account_id)
);

-- Invariant checked by tests and a periodic job: every journal sums to zero,
-- and every account's balance equals the sum of its entries.
SELECT journal_id FROM entries GROUP BY journal_id HAVING sum(amount) <> 0;
SELECT a.id FROM accounts a JOIN entries e ON e.account_id = a.id
GROUP BY a.id, a.balance HAVING a.balance <> sum(e.amount);
