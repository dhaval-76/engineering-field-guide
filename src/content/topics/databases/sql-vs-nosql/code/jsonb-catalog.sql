-- Common columns as columns; category-specific attributes as JSONB.
CREATE TABLE products (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku         text NOT NULL UNIQUE,
    category    text NOT NULL,
    name        text NOT NULL,
    price       numeric(12,2) NOT NULL,
    attributes  jsonb NOT NULL DEFAULT '{}'
);

INSERT INTO products (sku, category, name, price, attributes) VALUES
  ('LAP-14', 'laptops',  'Air 14',     999.00, '{"ram_gb": 16, "cpu": "M4", "storage_gb": 512}'),
  ('TSH-RD', 'clothing', 'Basic tee',   19.00, '{"size": "M", "colour": "red", "material": "cotton"}');

-- Containment queries use a GIN index…
CREATE INDEX products_attrs_gin ON products USING gin (attributes jsonb_path_ops);
SELECT name, price FROM products WHERE attributes @> '{"colour": "red", "size": "M"}';

-- …and a hot attribute can get its own B-tree expression index for ranges and sorting.
CREATE INDEX products_ram_idx ON products (((attributes ->> 'ram_gb')::int)) WHERE category = 'laptops';
SELECT name FROM products
WHERE category = 'laptops' AND (attributes ->> 'ram_gb')::int >= 16
ORDER BY price;

-- Some validation still lives in the database:
ALTER TABLE products ADD CONSTRAINT laptops_have_ram
  CHECK (category <> 'laptops' OR attributes ? 'ram_gb');
