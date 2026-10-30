-- Build without blocking writes (PostgreSQL). Can't run inside a transaction block;
-- if it fails it leaves an INVALID index behind — drop it and retry.
CREATE INDEX CONCURRENTLY orders_customer_created_idx ON orders (customer_id, created_at DESC);
DROP INDEX CONCURRENTLY IF EXISTS orders_customer_idx;         -- now redundant: a prefix of the new one

-- Indexes never used since statistics were last reset (check every replica before dropping!)
SELECT s.relname AS table, s.indexrelname AS index, s.idx_scan,
       pg_size_pretty(pg_relation_size(s.indexrelid)) AS size
FROM pg_stat_user_indexes s
JOIN pg_index i ON i.indexrelid = s.indexrelid
WHERE s.idx_scan = 0 AND NOT i.indisunique                       -- keep unique/PK indexes: they enforce rules
ORDER BY pg_relation_size(s.indexrelid) DESC;

-- Tables read mostly by sequential scans (candidates for a missing index)
SELECT relname, seq_scan, seq_tup_read, idx_scan, n_live_tup
FROM pg_stat_user_tables
WHERE n_live_tup > 100000
ORDER BY seq_tup_read DESC
LIMIT 10;

-- The slowest queries overall (pg_stat_statements extension)
SELECT calls, round(mean_exec_time::numeric, 1) AS mean_ms, round(total_exec_time::numeric) AS total_ms, query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;

-- Refresh planner statistics after a bulk load
ANALYZE orders;
