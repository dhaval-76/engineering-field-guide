-- The application connects as a NON-owner role; policies apply to it.
CREATE ROLE app_user LOGIN;
GRANT SELECT, INSERT, UPDATE, DELETE ON projects, users TO app_user;

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects FORCE ROW LEVEL SECURITY;     -- applies to the table owner too

CREATE POLICY tenant_isolation ON projects
    USING      (tenant_id = current_setting('app.tenant_id')::bigint)   -- rows you can see/update/delete
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::bigint);  -- rows you may write

-- Per request, inside the transaction. The third argument (true) = LOCAL:
-- the setting disappears at COMMIT/ROLLBACK, so a pooled connection can't leak it.
BEGIN;
SELECT set_config('app.tenant_id', '42', true);
SELECT * FROM projects;                            -- only tenant 42's rows, even without WHERE
INSERT INTO projects (tenant_id, owner_id, name) VALUES (7, 1, 'x');   -- ERROR: violates row-level security
COMMIT;

-- With no setting at all, current_setting('app.tenant_id') raises an error → fails closed.
