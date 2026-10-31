-- 1. Row lock inside a transaction: simplest "lock" when the data lives in one database
BEGIN;
SELECT * FROM inventory WHERE sku = 'SKU-1' FOR UPDATE;      -- others block until COMMIT
UPDATE inventory SET qty = qty - 1 WHERE sku = 'SKU-1';
COMMIT;

-- 2. Advisory lock: a named lock with no table, released at transaction end
BEGIN;
-- true = we hold it
SELECT pg_try_advisory_xact_lock(hashtext('daily-report:2026-10-30'));
-- … do the work …
COMMIT;                                                       -- released automatically

-- 3. Lease table for leader election, with a fencing token
CREATE TABLE leases (
  name       text PRIMARY KEY,
  holder     text        NOT NULL,
  token      bigint      NOT NULL,
  expires_at timestamptz NOT NULL
);

-- Acquire or take over an expired lease; the RETURNING token is the fencing token
INSERT INTO leases (name, holder, token, expires_at)
VALUES ('billing-scheduler', $1, 1, now() + interval '15 seconds')
ON CONFLICT (name) DO UPDATE
  SET holder = EXCLUDED.holder, token = leases.token + 1, expires_at = EXCLUDED.expires_at
  WHERE leases.expires_at < now() OR leases.holder = EXCLUDED.holder
-- no row returned = someone else leads
RETURNING token;

-- Renew (heartbeat) every few seconds while leading
UPDATE leases SET expires_at = now() + interval '15 seconds'
WHERE name = 'billing-scheduler' AND holder = $1 AND token = $2;
