-- One logical table, physically split by month.
CREATE TABLE events (
    id          bigint GENERATED ALWAYS AS IDENTITY,
    tenant_id   bigint NOT NULL,
    type        text NOT NULL,
    payload     jsonb NOT NULL,
    created_at  timestamptz NOT NULL,
    PRIMARY KEY (id, created_at)            -- unique constraints must include the partition key
) PARTITION BY RANGE (created_at);

CREATE TABLE events_2026_03 PARTITION OF events
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE events_2026_04 PARTITION OF events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
-- (create future partitions ahead of time — pg_partman or a scheduled job)

-- Indexes declared on the parent are created on every partition.
CREATE INDEX ON events (tenant_id, created_at DESC);

-- Partition pruning: only events_2026_03 is scanned.
EXPLAIN SELECT count(*) FROM events
WHERE created_at >= '2026-03-10' AND created_at < '2026-03-11';

-- Retention in milliseconds instead of a multi-hour DELETE:
ALTER TABLE events DETACH PARTITION events_2026_03 CONCURRENTLY;
DROP TABLE events_2026_03;
