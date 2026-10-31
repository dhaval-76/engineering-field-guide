-- Goal: rename customers.fullname → customers.name with no downtime and safe rollbacks.

-- Release 1 — EXPAND: add the new column. Old code ignores it.
-- nullable: instant, no table rewrite
ALTER TABLE customers ADD COLUMN name text;
-- App v1.1 writes BOTH columns and reads fullname (rollback to v1.0 still works).

-- Backfill in small batches (a job, not inside the deploy), so locks stay short:
UPDATE customers SET name = fullname
WHERE id IN (SELECT id FROM customers WHERE name IS NULL LIMIT 5000);
-- …repeat until 0 rows

-- Release 2 — MIGRATE reads: app v1.2 reads name, still writes both.

-- Release 3 — CONTRACT: once no running version reads fullname.
-- validated quickly if added as NOT VALID CHECK first
ALTER TABLE customers ALTER COLUMN name SET NOT NULL;
ALTER TABLE customers DROP COLUMN fullname;             -- app v1.3 writes only name

-- Same idea for: changing a column's type, splitting a table, making a column required,
-- renaming an API field or an event field.
