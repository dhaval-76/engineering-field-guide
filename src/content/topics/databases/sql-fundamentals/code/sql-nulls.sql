-- customers.phone is nullable. Which rows does each query return?
SELECT * FROM customers WHERE phone = NULL;        -- none, ever: NULL = NULL is NULL, not true
SELECT * FROM customers WHERE phone IS NULL;       -- ✓ customers without a phone
SELECT * FROM customers WHERE phone <> '555-0100'; -- skips customers whose phone is NULL!
SELECT * FROM customers WHERE phone IS DISTINCT FROM '555-0100';   -- ✓ includes NULLs

-- Aggregates ignore NULLs; over zero rows they return NULL.
SELECT COUNT(*)              AS customers,          -- every row
       COUNT(phone)          AS with_phone,         -- non-NULL phones only
       COALESCE(SUM(o.total), 0) AS revenue         -- 0 instead of NULL when there are no orders
FROM customers c LEFT JOIN orders o ON o.customer_id = c.id;

-- The NOT IN trap: if the subquery yields a single NULL, the whole predicate is never true.
SELECT * FROM products
WHERE id NOT IN (SELECT product_id FROM wishlist);  -- wishlist.product_id has a NULL → 0 rows

SELECT * FROM products p                             -- ✓ NOT EXISTS ignores NULLs correctly
WHERE NOT EXISTS (SELECT 1 FROM wishlist w WHERE w.product_id = p.id);

-- NULLs in sorting: PostgreSQL puts them last for ASC, first for DESC. Say what you want.
SELECT name, phone FROM customers ORDER BY phone NULLS LAST;
