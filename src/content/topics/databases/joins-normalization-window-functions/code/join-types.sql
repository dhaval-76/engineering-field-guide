-- INNER: orders with their customer. An order whose customer_id matches nothing disappears.
SELECT o.id, c.name, o.total
FROM orders o
JOIN customers c ON c.id = o.customer_id;

-- LEFT: every customer with their number of orders — including customers with none.
SELECT c.id, c.name, COUNT(o.id) AS orders        -- COUNT(o.id), not COUNT(*): NULLs aren't orders
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name;

-- LEFT with a condition on the right table: it belongs in ON …
SELECT c.name, COUNT(o.id) AS paid_orders_2026
FROM customers c
LEFT JOIN orders o
  ON o.customer_id = c.id
 AND o.status = 'PAID'
 AND o.created_at >= '2026-01-01'
GROUP BY c.id, c.name;                             -- customers with no paid orders → 0
-- … because WHERE o.status = 'PAID' would drop the NULL rows and turn this into an INNER JOIN.

-- FULL OUTER: reconcile our payments with the provider's settlement file.
SELECT p.id AS ours, s.payment_ref AS theirs, p.amount, s.amount AS settled
FROM payments p
FULL JOIN settlements s ON s.payment_ref = p.id
WHERE p.id IS NULL OR s.payment_ref IS NULL OR p.amount <> s.amount;   -- only the mismatches

-- SELF: each employee with their manager (LEFT so the CEO, with no manager, stays).
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON m.id = e.manager_id;

-- CROSS: one row per store per day, so days with zero sales still appear.
SELECT s.id AS store_id, d::date AS day, COALESCE(SUM(o.total), 0) AS revenue
FROM stores s
CROSS JOIN generate_series('2026-03-01'::date, '2026-03-31', interval '1 day') AS d
LEFT JOIN orders o ON o.store_id = s.id AND o.created_at::date = d::date
GROUP BY s.id, d
ORDER BY s.id, d;
