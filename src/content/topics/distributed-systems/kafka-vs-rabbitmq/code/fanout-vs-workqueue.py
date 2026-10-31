# ── Kafka (confluent-kafka): one event, many independent consumers — one consumer GROUP
# per service ──
from confluent_kafka import Consumer

def run_group(group_id: str, handle) -> None:
    c = Consumer({"bootstrap.servers": "kafka:9092", "group.id": group_id, "auto.offset.reset": "earliest"})
    c.subscribe(["order-events"])                        # every group gets every event
    while True:
        msg = c.poll(1.0)
        if msg is not None and not msg.error():
            handle(msg.value())

# run_group("inventory", reserve); run_group("email", send_confirmation)   — separate
# processes A new "analytics" group with auto.offset.reset=earliest replays the retained
# history.

# ── RabbitMQ (pika): a work queue — each job goes to ONE worker ──
import json
import pika

ch = pika.BlockingConnection(pika.URLParameters(AMQP_URL)).channel()
ch.queue_declare("thumbnails", durable=True, arguments={
    # poison message → DLQ after 5 tries
    "x-queue-type": "quorum", "x-delivery-limit": 5,
    "x-dead-letter-exchange": "dlx", "x-dead-letter-routing-key": "thumbnails.failed"})
# limits in-flight jobs per worker
ch.basic_qos(prefetch_count=4)

def on_job(channel, method, props, body):
    try:
        resize(json.loads(body)["image_id"], 320)
        channel.basic_ack(method.delivery_tag)
    except Exception:
        # redelivered (counted toward the limit)
        channel.basic_nack(method.delivery_tag, requeue=True)

ch.basic_consume("thumbnails", on_job)
# Producer: default exchange → queue
ch.basic_publish("", "thumbnails", json.dumps({"image_id": image_id}), pika.BasicProperties(delivery_mode=2))
