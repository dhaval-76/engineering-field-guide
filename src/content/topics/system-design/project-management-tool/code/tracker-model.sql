CREATE TABLE workflows (
    tenant_id   bigint NOT NULL,
    id          bigint NOT NULL,
    name        text NOT NULL,
    PRIMARY KEY (tenant_id, id)
);
CREATE TABLE workflow_transitions (
    tenant_id     bigint NOT NULL,
    workflow_id   bigint NOT NULL,
    from_status   text NOT NULL,
    to_status     text NOT NULL,
    required_role text,                          -- who may perform it
    -- e.g. {"requiredFields": ["resolution"]}
    rules         jsonb NOT NULL DEFAULT '{}',
    PRIMARY KEY (tenant_id, workflow_id, from_status, to_status)
);

CREATE TABLE issues (
    tenant_id     bigint NOT NULL,
    id            bigint NOT NULL,
    project_id    bigint NOT NULL,
    key           text NOT NULL,                 -- 'ENG-40'
    type          text NOT NULL,
    title         text NOT NULL,
    status        text NOT NULL,
    assignee_id   bigint,
    sprint_id     bigint,
    rank          text NOT NULL,                 -- ordering within a board column
    custom_fields jsonb NOT NULL DEFAULT '{}',
    version       int NOT NULL DEFAULT 0,        -- optimistic concurrency
    updated_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id),
    UNIQUE (tenant_id, key)
);
CREATE INDEX issues_board ON issues (tenant_id, sprint_id, status, rank);

CREATE TABLE issue_history (
    tenant_id  bigint NOT NULL,
    issue_id   bigint NOT NULL,
    at         timestamptz NOT NULL DEFAULT now(),
    actor_id   bigint NOT NULL,
    field      text NOT NULL,
    old_value  text,
    new_value  text
);
