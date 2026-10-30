CREATE TABLE customers (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       text NOT NULL UNIQUE,                    -- the DB, not the app, guarantees no duplicates
    name        text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku         text NOT NULL UNIQUE,
    name        text NOT NULL,
    price       numeric(12,2) NOT NULL CHECK (price >= 0),   -- exact decimal, never float
    stock       integer NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

CREATE TABLE orders (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES customers(id),  -- can't point at a missing customer
    status      text NOT NULL DEFAULT 'PLACED'
                CHECK (status IN ('PLACED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total       numeric(12,2) NOT NULL CHECK (total >= 0),
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    order_id    bigint  NOT NULL REFERENCES orders(id) ON DELETE CASCADE,  -- items die with the order
    product_id  bigint  NOT NULL REFERENCES products(id),                  -- but a product in use can't be deleted
    qty         integer NOT NULL CHECK (qty > 0),
    unit_price  numeric(12,2) NOT NULL,                  -- price at purchase time, deliberately copied
    PRIMARY KEY (order_id, product_id)
);

-- PostgreSQL does NOT index foreign-key columns automatically; joins and cascades need one.
CREATE INDEX ON orders (customer_id);
CREATE INDEX ON order_items (product_id);

-- Changing structure later: add nullable or with a default (fast in PostgreSQL 11+),
-- backfill, then tighten. Keep these in migration files (Flyway, Liquibase), not run by hand.
ALTER TABLE customers ADD COLUMN phone text;
