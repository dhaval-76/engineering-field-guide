-- A job queue in a table: many workers, each job processed once.
CREATE TABLE jobs (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payload    jsonb NOT NULL,
    status     text NOT NULL DEFAULT 'READY',
    run_after  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX jobs_ready_idx ON jobs (run_after) WHERE status = 'READY';

-- Each worker, in its own short transaction:
BEGIN;
SELECT id, payload FROM jobs
WHERE status = 'READY' AND run_after <= now()
ORDER BY run_after
LIMIT 10
FOR UPDATE SKIP LOCKED;          -- rows locked by other workers are skipped, not waited for
UPDATE jobs SET status = 'RUNNING' WHERE id = ANY($1);   -- the ids just selected
COMMIT;                          -- then do the work outside the transaction, and mark DONE/FAILED

-- Fail fast instead of queueing behind a long transaction:
SELECT * FROM accounts WHERE id = 1 FOR UPDATE NOWAIT;   -- ERROR 55P03 if locked
SET lock_timeout = '2s';                                  -- or wait at most 2 seconds

-- Protect a parent row from changes while inserting children (others can still read it):
SELECT * FROM products WHERE id = 7 FOR SHARE;

-- Advisory lock: a named mutex, e.g. "only one instance runs the nightly report".
SELECT pg_try_advisory_lock(hashtext('nightly-report'));   -- true → we own it
-- … run …
SELECT pg_advisory_unlock(hashtext('nightly-report'));
-- (pg_advisory_xact_lock releases automatically at commit)
