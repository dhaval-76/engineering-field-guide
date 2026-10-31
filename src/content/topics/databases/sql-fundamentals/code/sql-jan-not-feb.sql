-- Customers with orders in January 2026 but none in February 2026, with their January
-- count.
SELECT c.id, c.name, COUNT(*) AS jan_orders
FROM customers c
JOIN orders o
  ON o.customer_id = c.id
 -- half-open: all of Jan 31
 AND o.created_at >= '2026-01-01' AND o.created_at < '2026-02-01'
WHERE NOT EXISTS (
    SELECT 1 FROM orders f
    WHERE f.customer_id = c.id
      AND f.created_at >= '2026-02-01' AND f.created_at < '2026-03-01'
)
GROUP BY c.id, c.name
ORDER BY jan_orders DESC, c.id;

-- The LEFT JOIN February … WHERE feb.id IS NULL version works too: the IS NULL filter
-- removes every January row that was multiplied by a February match. NOT EXISTS states
-- the intent directly and can't inflate the count if someone later changes the filter.
