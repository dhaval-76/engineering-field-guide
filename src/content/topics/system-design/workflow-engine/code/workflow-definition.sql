CREATE TABLE workflow_versions (
    workflow_id  text NOT NULL,
    version      int NOT NULL,
    tenant_id    bigint NOT NULL,
    definition   jsonb NOT NULL,            -- immutable once published
    PRIMARY KEY (workflow_id, version)
);

CREATE TABLE runs (
    id           uuid PRIMARY KEY,
    tenant_id    bigint NOT NULL,
    workflow_id  text NOT NULL,
    version      int NOT NULL,              -- a run keeps the version it started with
    -- source event id: the same email can't start two runs
    trigger_key  text NOT NULL,
    -- RUNNING, WAITING, SUCCEEDED, FAILED, CANCELLED
    status       text NOT NULL,
    context      jsonb NOT NULL DEFAULT '{}',
    started_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (workflow_id, trigger_key)
);

CREATE TABLE step_results (
    run_id       uuid NOT NULL REFERENCES runs(id),
    step_id      text NOT NULL,
    status       text NOT NULL,             -- DONE, FAILED, SKIPPED, PARKED
    attempts     int NOT NULL DEFAULT 0,
    output       jsonb,
    error        text,
    wake_at      timestamptz,               -- parked steps (Retry-After, delays)
    PRIMARY KEY (run_id, step_id)
);
