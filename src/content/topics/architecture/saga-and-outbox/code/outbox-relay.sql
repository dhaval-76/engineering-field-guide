-- Outbox written in the same transaction as the business change
CREATE TABLE outbox (
  id            bigserial PRIMARY KEY,
  aggregate_id  text        NOT NULL,          -- used as the Kafka key → per-order ordering
  event_type    text        NOT NULL,
  payload       jsonb       NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now(),
  published_at  timestamptz
);
CREATE INDEX outbox_unpublished ON outbox (id) WHERE published_at IS NULL;

-- Relay: claim a batch; SKIP LOCKED lets several relay instances run without double-claiming
BEGIN;
SELECT id, aggregate_id, event_type, payload
FROM outbox
WHERE published_at IS NULL
ORDER BY id
LIMIT 100
FOR UPDATE SKIP LOCKED;
-- … publish each row to the broker and wait for acks …
UPDATE outbox SET published_at = now() WHERE id = ANY($1);
COMMIT;

-- Consumer side: the inbox makes handling idempotent (at-least-once delivery → exactly-once effect)
CREATE TABLE inbox (
  consumer   text NOT NULL,
  event_id   text NOT NULL,
  handled_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (consumer, event_id)
);
BEGIN;
INSERT INTO inbox (consumer, event_id) VALUES ('shipping', $1) ON CONFLICT DO NOTHING;
-- 0 rows inserted → duplicate: skip. 1 row → apply the business change in this same transaction.
COMMIT;
