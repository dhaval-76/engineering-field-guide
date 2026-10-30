-- Subquery: products priced above the average.
SELECT name, price
FROM products
WHERE price > (SELECT AVG(price) FROM products);

-- CTE: the same steps, named and read top to bottom.
WITH paid AS (
    SELECT customer_id, SUM(total) AS spent
    FROM orders
    WHERE status = 'PAID' AND created_at >= now() - interval '90 days'
    GROUP BY customer_id
),
vip AS (
    SELECT customer_id FROM paid WHERE spent >= 1000
)
SELECT c.name, p.spent
FROM vip v
JOIN paid p      ON p.customer_id = v.customer_id
JOIN customers c ON c.id = v.customer_id
ORDER BY p.spent DESC;

-- Recursive CTE: everyone under manager 7, with their depth in the tree.
WITH RECURSIVE reports AS (
    SELECT id, name, manager_id, 1 AS depth
    FROM employees
    WHERE manager_id = 7                        -- anchor: direct reports
  UNION ALL
    SELECT e.id, e.name, e.manager_id, r.depth + 1
    FROM employees e
    JOIN reports r ON e.manager_id = r.id       -- step: reports of the previous level
    WHERE r.depth < 20                          -- guard against cycles in bad data
)
SELECT * FROM reports ORDER BY depth, name;
