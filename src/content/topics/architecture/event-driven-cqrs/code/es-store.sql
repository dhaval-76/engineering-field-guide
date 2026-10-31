-- Append-only event store: one row per event, ordered per stream (aggregate)
CREATE TABLE events (
  global_position bigserial   PRIMARY KEY,       -- total order for projections
  stream_id       text        NOT NULL,          -- e.g. 'account-42'
  version         int         NOT NULL,          -- 1, 2, 3 … within the stream
  type            text        NOT NULL,
  data            jsonb       NOT NULL,
  metadata        jsonb       NOT NULL DEFAULT '{}',   -- causation id, correlation id, user
  recorded_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (stream_id, version)                    -- optimistic concurrency
);

-- Load a stream
SELECT type, data FROM events WHERE stream_id = $1 ORDER BY version;

-- Append with expected version: the writer read version 7, so it writes 8.
-- A concurrent writer that also read 7 violates UNIQUE → reload and retry the command.
INSERT INTO events (stream_id, version, type, data) VALUES ($1, 8, 'Withdrawn', '{"cents": 2500}');

-- Projection: a read model plus a checkpoint, updated in one transaction
CREATE TABLE account_balance (account_id text PRIMARY KEY, owner text, balance_cents bigint NOT NULL);
CREATE TABLE projection_checkpoint (name text PRIMARY KEY, position bigint NOT NULL);

SELECT global_position, stream_id, type, data FROM events
WHERE global_position > (SELECT position FROM projection_checkpoint WHERE name = 'balances')
ORDER BY global_position LIMIT 500;
-- apply each event to account_balance, then:
UPDATE projection_checkpoint SET position = $last WHERE name = 'balances';
