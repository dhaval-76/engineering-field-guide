# Place an order and record its event in ONE local transaction (psycopg 3).
from psycopg.types.json import Jsonb

def place_order(conn, cmd: dict) -> dict:
    with conn.transaction():
        order_id, customer_id, total = conn.execute(
            "INSERT INTO orders (customer_id, total) VALUES (%s, %s) RETURNING id, customer_id, total",
            (cmd["customer_id"], cmd["total"])).fetchone()
        conn.execute(
            "INSERT INTO outbox (id, aggregate_type, aggregate_id, type, payload) "
            "VALUES (gen_random_uuid(), 'Order', %s, 'OrderPlaced', %s)",          # event id: consumers de-duplicate
            (str(order_id), Jsonb({"order_id": order_id, "customer_id": customer_id, "total": total})))
    return {"id": order_id, "customer_id": customer_id, "total": total}       # ✗ never publish inside the tx
# The relay (SELECT … FOR UPDATE SKIP LOCKED → publish → mark sent) is shown in Sagas, outbox and inbox.
# Delivery is at-least-once: consumers must be idempotent.
