-- PARTIAL: only the rows the hot query looks at. 2% of orders are PENDING → a tiny index.
CREATE INDEX orders_pending_idx ON orders (created_at) WHERE status = 'PENDING';
-- used by: SELECT … FROM orders WHERE status = 'PENDING' ORDER BY created_at LIMIT 100;
-- Also great for soft deletes and uniqueness among live rows:
CREATE UNIQUE INDEX users_email_live_uq ON users (lower(email)) WHERE deleted_at IS NULL;

-- EXPRESSION: index exactly what the WHERE clause computes.
CREATE INDEX users_email_lower_idx ON users (lower(email));
SELECT * FROM users WHERE lower(email) = lower($1);          -- ✓ matches the expression
-- For dates: rewrite instead of indexing created_at::date
SELECT * FROM orders WHERE created_at >= '2026-03-01' AND created_at < '2026-03-02';

-- TRIGRAM (pg_trgm) + GIN: substring and fuzzy search, including leading wildcards.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX products_name_trgm_idx ON products USING gin (name gin_trgm_ops);
SELECT * FROM products WHERE name ILIKE '%phone%';

-- JSONB + GIN: containment queries on documents.
CREATE INDEX products_attrs_idx ON products USING gin (attributes jsonb_path_ops);
SELECT * FROM products WHERE attributes @> '{"colour": "black", "wireless": true}';

-- BRIN: a few KB summarising min/max per block range of a huge, time-ordered table.
CREATE INDEX events_created_brin ON events USING brin (created_at);
SELECT count(*) FROM events WHERE created_at >= now() - interval '1 hour';

-- Prefix LIKE with a B-tree needs a pattern-compatible operator class (non-C collations):
CREATE INDEX customers_name_prefix_idx ON customers (name text_pattern_ops);
SELECT * FROM customers WHERE name LIKE 'Ash%';
