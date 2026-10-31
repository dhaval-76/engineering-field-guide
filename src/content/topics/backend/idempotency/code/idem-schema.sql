CREATE TABLE idempotency_keys (
    key            text        NOT NULL,
    -- keys are unique per client, not globally
    client_id      text        NOT NULL,
    -- same key + different body = client bug → 422
    request_hash   text        NOT NULL,
    status         text        NOT NULL,              -- IN_PROGRESS | DONE
    response_code  int,
    response_body  jsonb,
    -- lets a crashed IN_PROGRESS attempt be retried
    locked_until   timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (client_id, key)
);
-- Purge after the retry window (e.g. 24 h – 7 days): DELETE … WHERE created_at < now() -
-- interval '24 hours'
