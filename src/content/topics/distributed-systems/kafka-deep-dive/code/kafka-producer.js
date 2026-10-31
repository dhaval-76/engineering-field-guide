// KafkaJS
import { Kafka, CompressionTypes } from 'kafkajs';

const kafka = new Kafka({ clientId: 'orders', brokers: ['kafka-1:9092', 'kafka-2:9092'] });
const producer = kafka.producer({ idempotent: true, maxInFlightRequests: 5 });
await producer.connect();

export async function publish(event) {
  await producer.send({
    topic: 'order-events',
    acks: -1,                               // all in-sync replicas
    compression: CompressionTypes.GZIP,
    messages: [{ key: event.orderId, value: JSON.stringify(event) }],
  });
}
