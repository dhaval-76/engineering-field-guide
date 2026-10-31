CREATE TABLE outbox (
    id              uuid PRIMARY KEY,
    aggregate_type  text NOT NULL,
    aggregate_id    text NOT NULL,
    type            text NOT NULL,
    payload         jsonb NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz
);
CREATE INDEX outbox_unsent ON outbox (created_at) WHERE sent_at IS NULL;

-- The relay's claim query
SELECT * FROM outbox
WHERE sent_at IS NULL
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;

-- Consumer side: remember processed events in the same transaction as the effect.
CREATE TABLE processed_events (
    event_id     uuid PRIMARY KEY,
    processed_at timestamptz NOT NULL DEFAULT now()
);
-- INSERT INTO processed_events (event_id) VALUES ($1) ON CONFLICT DO NOTHING;
-- 0 rows inserted → already handled → skip.
