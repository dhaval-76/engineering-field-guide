-- ✗ OFFSET: the database finds and throws away 50,000 rows to return 20
SELECT id, created_at, total FROM orders
WHERE customer_id = 'c_1'
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 50000;

-- ✓ Keyset (cursor): seek straight to the last row the client saw, then read 20
SELECT id, created_at, total FROM orders
WHERE customer_id = 'c_1'
  AND (created_at, id) < ('2026-10-01 09:30:00', 'ord_8841')   -- values from the cursor
ORDER BY created_at DESC, id DESC
LIMIT 21;                                                       -- one extra row → is there a next page?

-- The index must match the filter + sort so the seek is a range scan:
CREATE INDEX orders_customer_created_id ON orders (customer_id, created_at DESC, id DESC);

-- Row-value comparison (a, b) < (x, y) works in PostgreSQL, MySQL 8 and SQLite.
-- Elsewhere expand it: created_at < :t OR (created_at = :t AND id < :id)
