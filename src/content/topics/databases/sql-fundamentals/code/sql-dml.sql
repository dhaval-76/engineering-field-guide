-- Insert several rows in one statement; get generated ids back.
INSERT INTO products (sku, name, price, stock)
VALUES ('BOOK-1', 'SQL Pocket Guide', 24.99, 10),
       ('PEN-BLK', 'Black pen', 1.20, 500)
RETURNING id, sku;

-- Upsert: the conflict target must be a UNIQUE column or constraint.
INSERT INTO products (sku, name, price, stock)
VALUES ('PEN-BLK', 'Black pen', 1.10, 50)
ON CONFLICT (sku) DO UPDATE
    SET price = EXCLUDED.price,               -- EXCLUDED = the row we tried to insert
        stock = products.stock + EXCLUDED.stock;
-- MySQL: INSERT … ON DUPLICATE KEY UPDATE price = VALUES(price), …

-- State transition as one atomic statement: only a PLACED order can be paid.
UPDATE orders
SET    status = 'PAID'
WHERE  id = 101 AND status = 'PLACED'
RETURNING id, status;                         -- no row back → it wasn't PLACED any more

-- Update from another table: reprice open orders' items.
UPDATE order_items oi
SET    unit_price = p.price
FROM   products p, orders o
WHERE  oi.product_id = p.id AND oi.order_id = o.id AND o.status = 'PLACED';

-- Delete: children go with ON DELETE CASCADE; otherwise delete them first.
DELETE FROM orders WHERE id = 101;

-- Before a risky change in a console, do it inside a transaction and look at the count.
BEGIN;
UPDATE products SET price = price * 1.10 WHERE sku LIKE 'BOOK-%';   -- "UPDATE 37"?
-- ROLLBACK; if the count is not what you expected
COMMIT;
