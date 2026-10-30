-- The query an orders screen runs thousands of times a minute:
SELECT id, status, total, created_at
FROM orders
WHERE customer_id = $1 AND status = 'PAID'
ORDER BY created_at DESC
LIMIT 20;

-- ✗ Two single-column indexes: the planner picks one (or bitmap-ANDs both),
--   then still sorts all of that customer's paid orders to find the newest 20.
CREATE INDEX ON orders (customer_id);
CREATE INDEX ON orders (status);

-- ✓ One composite index shaped like the query: equality, equality, then the sort column.
CREATE INDEX orders_customer_status_created_idx
    ON orders (customer_id, status, created_at DESC);
-- Seek to (customer 42, PAID), read entries already in created_at order, stop after 20.

-- ✗ Range column first: entries for one customer are scattered across the whole date range.
CREATE INDEX ON orders (created_at, customer_id);

-- Which column first among equality columns? It matters less than people think for a
-- query that uses both; choose so that OTHER queries can reuse the leftmost prefix
-- (here, "all orders of a customer" also uses this index).
