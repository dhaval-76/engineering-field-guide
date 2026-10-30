-- UNNORMALIZED: one row per order, items squeezed into columns.
-- order_id | customer_id | customer_name | customer_city | item1 | qty1 | item2 | qty2 | item3 | qty3
-- A 4th item needs a schema change; "who bought pens?" needs to check three columns.

-- 1NF: one value per cell, no repeating groups → one row per order line.
-- order_id | product_id | customer_id | customer_name | customer_city | product_name | unit_price | qty
-- Key: (order_id, product_id). Customer details repeat on every line of every order.

-- 2NF: no column may depend on only PART of the key (order_id, product_id).
--   product_name depends on product_id alone       → products table
--   customer_* depend on order_id alone            → orders table
-- 3NF: no column may depend on another non-key column.
--   customer_name, customer_city depend on customer_id, not on the order → customers table

CREATE TABLE customers (
    id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL,
    city text NOT NULL
);
CREATE TABLE products (
    id    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  text NOT NULL,
    price numeric(12,2) NOT NULL              -- the CURRENT price
);
CREATE TABLE orders (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES customers(id),
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE order_items (
    order_id   bigint NOT NULL REFERENCES orders(id),
    product_id bigint NOT NULL REFERENCES products(id),
    qty        integer NOT NULL CHECK (qty > 0),
    unit_price numeric(12,2) NOT NULL,        -- the price AT PURCHASE: a different fact, not a copy
    PRIMARY KEY (order_id, product_id)
);
-- Moving city: one UPDATE on one row. Renaming a product: one row. No anomalies.

-- Deliberate denormalization for a hot list screen, kept in sync in the same transaction:
ALTER TABLE customers ADD COLUMN order_count integer NOT NULL DEFAULT 0;
-- on each new order:  UPDATE customers SET order_count = order_count + 1 WHERE id = :customerId;
