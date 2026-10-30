-- 1. A million rows of realistic-ish data
CREATE TABLE orders_demo AS
SELECT g AS id,
       (random() * 50000)::int AS customer_id,
       (ARRAY['PLACED','PAID','SHIPPED','DELIVERED'])[1 + (random() * 3)::int] AS status,
       round((random() * 500)::numeric, 2) AS total,
       now() - (random() * interval '365 days') AS created_at
FROM generate_series(1, 1000000) g;
ANALYZE orders_demo;

-- 2. Baseline: expect Seq Scan with ~999,980 rows removed by filter, tens of ms or more
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, total FROM orders_demo WHERE customer_id = 4242 ORDER BY created_at DESC LIMIT 20;

-- 3. Index shaped for the query, then the same EXPLAIN: Index Scan, no Sort, well under 1 ms
CREATE INDEX ON orders_demo (customer_id, created_at DESC);
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, total FROM orders_demo WHERE customer_id = 4242 ORDER BY created_at DESC LIMIT 20;

-- 4. Low selectivity: ~25% of rows match, so the planner keeps a Seq Scan even with an index
CREATE INDEX ON orders_demo (status);
EXPLAIN ANALYZE SELECT count(*) FROM orders_demo WHERE status = 'PAID';

-- 5. A function on the column defeats the index…
EXPLAIN ANALYZE SELECT * FROM orders_demo WHERE created_at::date = current_date - 1;
-- …a half-open range on the raw column doesn't (with an index on created_at)
CREATE INDEX ON orders_demo (created_at);
EXPLAIN ANALYZE SELECT * FROM orders_demo
WHERE created_at >= current_date - 1 AND created_at < current_date;
