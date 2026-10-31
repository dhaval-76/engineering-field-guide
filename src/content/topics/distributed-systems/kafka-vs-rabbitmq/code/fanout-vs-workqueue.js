// ── Kafka (kafkajs): one event, many independent consumers — one consumer GROUP per service ──
const inventory = kafka.consumer({ groupId: 'inventory' }); // gets every event
await inventory.subscribe({ topic: 'order-events' });
await inventory.run({ eachMessage: async ({ message }) => reserve(JSON.parse(message.value)) });

const email = kafka.consumer({ groupId: 'email' });         // also gets every event
await email.subscribe({ topic: 'order-events' });
await email.run({ eachMessage: async ({ message }) => sendConfirmation(JSON.parse(message.value)) });
// A new "analytics" group subscribing with fromBeginning: true replays the retained history.

// ── RabbitMQ (amqplib): a work queue — each job goes to ONE worker ──
const ch = await (await amqp.connect(process.env.AMQP_URL)).createChannel();
await ch.assertQueue('thumbnails', {
  durable: true,
  arguments: { 'x-queue-type': 'quorum', 'x-delivery-limit': 5,        // poison message → DLQ after 5 tries
               'x-dead-letter-exchange': 'dlx', 'x-dead-letter-routing-key': 'thumbnails.failed' },
});
await ch.prefetch(4);                                         // limits in-flight jobs per worker
await ch.consume('thumbnails', async (msg) => {
  try {
    await resize(JSON.parse(msg.content).imageId, 320);
    ch.ack(msg);
  } catch {
    ch.nack(msg, false, true);                                // requeue → redelivered (counted toward the limit)
  }
});

// Producer: default exchange → queue
ch.sendToQueue('thumbnails', Buffer.from(JSON.stringify({ imageId })), { persistent: true });
