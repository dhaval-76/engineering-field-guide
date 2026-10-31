-- 1. Second-highest DISTINCT salary in each department (ties share a rank).
SELECT department_id, name, salary
FROM (
    SELECT e.*, DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rnk
    FROM employees e
) t
WHERE rnk = 2;

-- 2. Top 3 players per game; tied scores share a position.
SELECT game_id, user_id, score, pos
FROM (
    SELECT l.*, DENSE_RANK() OVER (PARTITION BY game_id ORDER BY score DESC) AS pos
    FROM leaderboard l
) t
WHERE pos <= 3
ORDER BY game_id, pos, user_id;

-- 3. Orders above that customer's own average.
SELECT id, customer_id, total, customer_avg
FROM (
    SELECT o.*, AVG(total) OVER (PARTITION BY customer_id) AS customer_avg
    FROM orders o
) t
WHERE total > customer_avg;

-- 4. Monthly revenue and change from the previous month.
WITH monthly AS (
    SELECT date_trunc('month', created_at) AS month, SUM(total) AS revenue
    FROM orders
    WHERE status <> 'CANCELLED'
    GROUP BY 1
)
SELECT month, revenue,
       revenue - LAG(revenue) OVER (ORDER BY month)                             AS change,
       ROUND(100.0 * (revenue / NULLIF(LAG(revenue) OVER (ORDER BY month), 0) - 1), 1) AS pct_change
FROM monthly
ORDER BY month;

-- 5. Gaps and islands: each user's streaks of consecutive login days.
-- Consecutive dates minus their row number give the same "anchor" date within a streak.
WITH days AS (
    SELECT DISTINCT user_id, login_at::date AS day FROM logins
),
grouped AS (
    SELECT user_id, day,
           day - (ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY day))::int AS anchor
    FROM days
)
SELECT user_id, MIN(day) AS streak_start, MAX(day) AS streak_end, COUNT(*) AS days
FROM grouped
GROUP BY user_id, anchor
HAVING COUNT(*) >= 3                  -- streaks of at least three days
ORDER BY user_id, streak_start;
