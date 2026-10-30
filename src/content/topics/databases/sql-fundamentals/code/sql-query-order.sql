SELECT   customer, SUM(total) AS spent           -- 5. compute output columns, define aliases
FROM     orders                                  -- 1. start from all rows
WHERE    status = 'PAID'                         -- 2. keep rows
GROUP BY customer                                -- 3. form groups
HAVING   SUM(total) > 100                        -- 4. keep groups
ORDER BY spent DESC, customer                    -- 6. sort (aliases allowed here) — with a tiebreaker
LIMIT    2;                                      -- 7. take the first rows

-- ✗ WHERE spent > 100           → column "spent" does not exist (aliases come later)
-- ✗ WHERE SUM(total) > 100      → aggregates not allowed in WHERE (no groups yet)
-- ✗ SELECT customer, status, SUM(total) … GROUP BY customer
--                               → "status" must appear in GROUP BY or an aggregate

-- Filtering a SELECT alias: wrap it, or use a CTE.
WITH spending AS (
    SELECT customer, SUM(total) AS spent FROM orders WHERE status = 'PAID' GROUP BY customer
)
SELECT * FROM spending WHERE spent > 100;
