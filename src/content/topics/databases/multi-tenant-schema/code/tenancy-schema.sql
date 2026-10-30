CREATE TABLE tenants (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug       text NOT NULL UNIQUE,
    tier       text NOT NULL CHECK (tier IN ('FREE', 'TEAM', 'ENTERPRISE')),
    region     text NOT NULL,
    database   text NOT NULL DEFAULT 'shared-1'      -- the registry: where this tenant's data lives
);

CREATE TABLE users (
    tenant_id  bigint NOT NULL REFERENCES tenants(id),
    id         bigint GENERATED ALWAYS AS IDENTITY,
    email      text NOT NULL,
    PRIMARY KEY (tenant_id, id),
    UNIQUE (tenant_id, email)                        -- unique per tenant, not globally
);

CREATE TABLE projects (
    tenant_id  bigint NOT NULL,
    id         bigint GENERATED ALWAYS AS IDENTITY,
    owner_id   bigint NOT NULL,
    name       text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id),
    -- composite FK: a project can only reference an owner in the SAME tenant
    FOREIGN KEY (tenant_id, owner_id) REFERENCES users (tenant_id, id)
);

-- Every index leads with tenant_id: every query is "within one tenant".
CREATE INDEX ON projects (tenant_id, created_at DESC);
