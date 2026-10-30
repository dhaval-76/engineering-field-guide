# confluent-kafka (librdkafka)
import json
from confluent_kafka import Producer

producer = Producer({
    "bootstrap.servers": "kafka-1:9092,kafka-2:9092",
    "acks": "all",
    "enable.idempotence": True,
    "linger.ms": 5,
    "compression.type": "lz4",
})

def on_delivery(err, msg):
    if err:
        print(f"delivery failed for {msg.key()}: {err}")   # log and alert

def publish(event: dict) -> None:
    producer.produce("order-events", key=event["orderId"], value=json.dumps(event), callback=on_delivery)
    producer.poll(0)          # serve delivery callbacks

# on shutdown: producer.flush()
