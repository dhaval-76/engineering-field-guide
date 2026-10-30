CREATE TABLE jobs (
    id            uuid PRIMARY KEY,
    partition     smallint NOT NULL,                 -- hash(id) % 64: which scheduler owns it
    type          text NOT NULL,
    payload       jsonb NOT NULL,
    priority      smallint NOT NULL DEFAULT 5,
    run_at        timestamptz NOT NULL,
    status        text NOT NULL DEFAULT 'PENDING',   -- PENDING, RUNNING, SUCCEEDED, FAILED, DEAD, CANCELLED
    attempts      int NOT NULL DEFAULT 0,
    max_attempts  int NOT NULL DEFAULT 5,
    lease_owner   text,
    lease_until   timestamptz,
    fence         bigint NOT NULL DEFAULT 0,          -- incremented on every claim
    schedule_id   uuid,                              -- set for occurrences of a recurring schedule
    scheduled_for timestamptz,
    last_error    text,
    UNIQUE (schedule_id, scheduled_for)              -- one occurrence per tick, even if created twice
);
-- "What's due in my partitions?" — a small partial index over runnable jobs only
CREATE INDEX jobs_due ON jobs (partition, priority DESC, run_at)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE TABLE schedules (
    id         uuid PRIMARY KEY,
    cron       text NOT NULL,                        -- '0 9 * * MON-FRI'
    time_zone  text NOT NULL,                        -- 'Asia/Kolkata' — DST-aware next-run computation
    job_type   text NOT NULL,
    payload    jsonb NOT NULL,
    overlap    text NOT NULL DEFAULT 'SKIP',          -- SKIP or QUEUE if the previous run is still active
    enabled    boolean NOT NULL DEFAULT true
);

CREATE TABLE job_dependencies (
    job_id      uuid NOT NULL REFERENCES jobs(id),
    depends_on  uuid NOT NULL REFERENCES jobs(id),
    PRIMARY KEY (job_id, depends_on)
);
