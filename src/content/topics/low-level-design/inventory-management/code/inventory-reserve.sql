CREATE TABLE stock (
  warehouse text NOT NULL,
  sku       text NOT NULL,
  on_hand   int  NOT NULL CHECK (on_hand >= 0),
  reserved  int  NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= on_hand),
  PRIMARY KEY (warehouse, sku)
);

CREATE TABLE reservation (
  id         uuid PRIMARY KEY,
  order_id   text NOT NULL,
  warehouse  text NOT NULL,
  sku        text NOT NULL,
  qty        int  NOT NULL CHECK (qty > 0),
  expires_at timestamptz NOT NULL,
  UNIQUE (order_id, sku)                       -- a retried checkout can't reserve twice
);

-- Reserve atomically: the WHERE clause is the oversell check; 0 rows updated = not enough stock here
UPDATE stock SET reserved = reserved + 2
WHERE warehouse = 'BLR' AND sku = 'SKU-1' AND on_hand - reserved >= 2;

-- Confirm (ship): stock leaves the building
UPDATE stock SET reserved = reserved - 2, on_hand = on_hand - 2
WHERE warehouse = 'BLR' AND sku = 'SKU-1';

-- Expiry job: release abandoned reservations in batches
WITH expired AS (
  DELETE FROM reservation WHERE expires_at < now()
  RETURNING warehouse, sku, qty
)
UPDATE stock s SET reserved = s.reserved - e.total
FROM (SELECT warehouse, sku, sum(qty) AS total FROM expired GROUP BY warehouse, sku) e
WHERE s.warehouse = e.warehouse AND s.sku = e.sku;
