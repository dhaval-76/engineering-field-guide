-- SEMI JOIN: customers who have ordered at least once — each customer once.
SELECT c.id, c.name
FROM customers c
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
-- A plain JOIN would repeat a customer once per order (and tempt you to add DISTINCT).

-- ANTI JOIN: customers who never ordered.
SELECT c.id, c.name
FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id);
-- Equivalent: LEFT JOIN orders o … WHERE o.id IS NULL
-- Avoid:      WHERE c.id NOT IN (SELECT customer_id FROM orders)  -- breaks on a NULL

-- BUG 1 — fan-out: an order with 3 items and 2 payments becomes 3 × 2 = 6 rows.
SELECT o.id, SUM(i.qty * i.unit_price) AS items_total, SUM(p.amount) AS paid   -- both inflated!
FROM orders o
JOIN order_items i ON i.order_id = o.id
JOIN payments p    ON p.order_id = o.id
GROUP BY o.id;

-- FIX — aggregate each child on its own, then join one row per order.
WITH items AS (
    SELECT order_id, SUM(qty * unit_price) AS items_total FROM order_items GROUP BY order_id
),
paid AS (
    SELECT order_id, SUM(amount) AS paid FROM payments GROUP BY order_id
)
SELECT o.id, i.items_total, COALESCE(p.paid, 0) AS paid
FROM orders o
JOIN items i     ON i.order_id = o.id
LEFT JOIN paid p ON p.order_id = o.id;
