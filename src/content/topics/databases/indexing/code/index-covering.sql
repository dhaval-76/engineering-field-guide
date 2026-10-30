-- List endpoint: newest orders of a customer since a date, only a few columns.
SELECT id, status, total
FROM orders
WHERE customer_id = $1 AND created_at > $2
ORDER BY created_at DESC
LIMIT 20;

CREATE INDEX CONCURRENTLY orders_customer_created_cover_idx
    ON orders (customer_id, created_at DESC)    -- 1. equality, then range + sort column
    INCLUDE (id, status, total);                -- 2. payload columns: stored in leaves, not searchable

-- Why:
-- customer_id first      → seek straight to this customer's entries
-- created_at DESC next   → the range and the ORDER BY are both satisfied by index order: no Sort node
-- INCLUDE (…)            → every selected column is in the index: Index Only Scan, no table visits
-- LIMIT 20               → stops after 20 leaf entries
--
-- EXPLAIN: Limit → Index Only Scan using orders_customer_created_cover_idx … Heap Fetches: 0
-- (MySQL has no INCLUDE: add the columns to the key instead, and InnoDB secondary
--  indexes already contain the primary key.)
