# confluent-kafka + psycopg: claim the event id and record the outcome in ONE transaction.
import json
from confluent_kafka import Consumer

consumer = Consumer({"bootstrap.servers": "kafka:9092", "group.id": "payments",
                     "enable.auto.commit": False, "auto.offset.reset": "earliest"})
consumer.subscribe(["payments.requested"])

while True:
    msg = consumer.poll(1.0)
    if msg is None or msg.error():
        continue
    e = json.loads(msg.value())
    with pool.connection() as conn, conn.transaction():
        claimed = conn.execute("INSERT INTO processed_events (event_id) VALUES (%s) ON CONFLICT DO NOTHING",
                               (e["event_id"],)).rowcount
        # duplicate deliveries skip straight to the commit
        if claimed == 1:
            # The external side effect carries its own idempotency key: a retry can't
            # double-charge.
            charge = gateway.charge(e["customer_id"], e["amount"], idempotency_key=f"payment-{e['event_id']}")
            conn.execute("INSERT INTO payments (event_id, charge_id, amount) VALUES (%s, %s, %s)",
                         (e["event_id"], charge.id, e["amount"]))
    # offset only after the database commit
    consumer.commit(message=msg, asynchronous=False)
