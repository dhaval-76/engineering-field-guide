// Two cache levels in front of the database.
import { LRUCache } from 'lru-cache';

// Level 1: in-process — microseconds, short TTL
const local = new LRUCache({ max: 50_000, ttl: 30_000 });

export async function getProduct(id) {
  let v = local.get(id);
  if (v) return v;
  // Level 2: shared across instances
  const cached = await redis.get(`product:v1:${id}`);
  if (cached) v = JSON.parse(cached);
  else {
    v = await repo.findView(id);
    if (!v) throw Object.assign(new Error(`product ${id}`), { status: 404 });
    await redis.set(`product:v1:${id}`, JSON.stringify(v), 'EX', 600);
  }
  local.set(id, v);
  return v;
}

// Shed load early, keep the request path fast; slow work goes to consumers of the outbox
// event.
app.post('/orders', async (req, res) => {
  if (!(await perClient.tryAcquire(req.get('X-Api-Key')))) return res.status(429).set('Retry-After', '1').end();
  // DB transaction + outbox row only
  res.status(201).json(await orders.place(req.body));
});

// Worker: retried independently; never slows checkout.
await consumer.run({ eachMessage: async ({ message }) => emails.sendConfirmation(JSON.parse(message.value)) });
