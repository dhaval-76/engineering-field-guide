-- Prefer operations that are idempotent by construction — then retries need no bookkeeping.

-- Set an absolute value instead of incrementing
UPDATE carts SET quantity = 3 WHERE cart_id = ? AND sku = ?;          -- repeatable
-- not: SET quantity = quantity + 1                                      -- each retry adds one

-- Upsert keyed by a natural or client-supplied id
INSERT INTO orders (id, customer_id, total) VALUES (?, ?, ?)
ON CONFLICT (id) DO NOTHING;

-- Conditional state transitions: only the first attempt changes anything
UPDATE orders SET status = 'SHIPPED', shipped_at = now()
WHERE id = ? AND status = 'PAID';                                       -- 0 rows on a repeat

-- Message consumers: remember processed message ids in the same transaction as the effect
INSERT INTO processed_messages (consumer, message_id) VALUES ('billing', ?)
ON CONFLICT DO NOTHING;                                                 -- 0 rows → already handled, skip
