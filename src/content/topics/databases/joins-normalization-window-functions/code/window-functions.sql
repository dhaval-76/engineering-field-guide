-- Latest order per customer: number rows inside each customer's window, keep #1.
SELECT *
FROM (
    SELECT o.*,
           ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC, id DESC) AS rn
    FROM orders o
) t
WHERE rn = 1;                 -- window results can't be filtered in WHERE of the same query
-- PostgreSQL shortcut: SELECT DISTINCT ON (customer_id) * FROM orders ORDER BY customer_id, created_at DESC;

-- Every order next to its customer's totals — no GROUP BY, rows are kept.
SELECT id, customer_id, total,
       SUM(total) OVER (PARTITION BY customer_id)                AS customer_total,
       ROUND(100.0 * total / SUM(total) OVER (PARTITION BY customer_id), 1) AS pct_of_customer,
       AVG(total) OVER ()                                        AS overall_avg
FROM orders;

-- Running total and 7-day moving average of daily revenue.
WITH daily AS (
    SELECT created_at::date AS day, SUM(total) AS revenue
    FROM orders GROUP BY 1
)
SELECT day, revenue,
       SUM(revenue) OVER (ORDER BY day ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total,
       AVG(revenue) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)         AS avg_7d
FROM daily
ORDER BY day;

-- Time since the customer's previous order.
SELECT customer_id, id, created_at,
       created_at - LAG(created_at) OVER (PARTITION BY customer_id ORDER BY created_at) AS since_previous
FROM orders;

-- Split customers into four spending bands.
SELECT customer_id, spent, NTILE(4) OVER (ORDER BY spent DESC) AS quartile
FROM (SELECT customer_id, SUM(total) AS spent FROM orders GROUP BY customer_id) s;
