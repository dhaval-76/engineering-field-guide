CREATE TABLE processed_events (
    event_id     uuid PRIMARY KEY,
    processed_at timestamptz NOT NULL DEFAULT now()
);
-- Keep only as long as redeliveries are possible (retention of the topic + margin):
DELETE FROM processed_events WHERE processed_at < now() - interval '14 days';
