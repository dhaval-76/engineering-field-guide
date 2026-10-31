# psycopg 3. Allowed transitions as data:
from psycopg.types.json import Jsonb

NEXT = {
    "PLACED": {"CONFIRMED", "CANCELLED"},
    "CONFIRMED": {"PREPARING", "CANCELLED"},
    "PREPARING": {"OUT_FOR_DELIVERY", "CANCELLED"},
    "OUT_FOR_DELIVERY": {"DELIVERED"},
    "DELIVERED": {"RETURNED"},
    "CANCELLED": set(),
    "RETURNED": set(),
}

class ConcurrentStatusChange(Exception):
    pass

class _Ignored(Exception):
    pass

def apply_event(conn, order_id: int, event_id: str, nxt: str, actor: str) -> bool:
    """Applies a partner event exactly once and only if it's a valid next step."""
    try:
        with conn.transaction():
            if conn.execute("INSERT INTO processed_events (event_id) VALUES (%s) ON CONFLICT DO NOTHING",
                            (event_id,)).rowcount == 0:
                raise _Ignored()                             # duplicate delivery
            (current,) = conn.execute("SELECT status FROM orders WHERE id = %s", (order_id,)).fetchone()
            if nxt not in NEXT[current]:
                raise _Ignored()                             # late or invalid event (log it)
            moved = conn.execute("UPDATE orders SET status = %s, updated_at = now() WHERE id = %s AND status = %s",
                                 (nxt, order_id, current)).rowcount
            if moved == 0:
                raise ConcurrentStatusChange(order_id)       # another actor won; caller retries
            conn.execute("INSERT INTO order_status_history (order_id, status, actor, at) VALUES (%s, %s, %s, now())",
                         (order_id, nxt, actor))
            conn.execute("INSERT INTO outbox (id, type, aggregate_id, payload) "
                         "VALUES (gen_random_uuid(), 'OrderStatusChanged', %s, %s)",
                         (str(order_id), Jsonb({"orderId": order_id, "status": nxt})))
        return True
    except _Ignored:
        return False                                         # rolled back: nothing recorded
