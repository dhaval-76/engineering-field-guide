# Polling relay with psycopg 3 + confluent-kafka. Several instances can run: SKIP LOCKED splits the work.
import psycopg
from confluent_kafka import Producer

producer = Producer({"bootstrap.servers": "kafka:9092", "acks": "all", "enable.idempotence": True})

def relay_once(conn: psycopg.Connection) -> int:
    with conn.transaction():
        rows = conn.execute(
            """SELECT id, aggregate_id, payload::text FROM outbox
               WHERE published_at IS NULL ORDER BY id LIMIT 100
               FOR UPDATE SKIP LOCKED""").fetchall()
        if not rows:
            return 0
        errors = []
        for id_, key, payload in rows:
            producer.produce("orders", key=key, value=payload, headers={"event-id": str(id_)},
                             on_delivery=lambda err, msg: err and errors.append(err))
        producer.flush()                                  # wait for broker acks
        if errors:
            raise RuntimeError(f"publish failed: {errors[0]}")   # rollback → retried later
        conn.execute("UPDATE outbox SET published_at = now() WHERE id = ANY(%s)", ([r[0] for r in rows],))
        return len(rows)
