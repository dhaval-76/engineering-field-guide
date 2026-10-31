// node-redis v4+
const k = `idem:${clientId}:${key}`;
const first = await redis.set(k, 'IN_PROGRESS', { NX: true, EX: 30 });
if (first === null) {
  const stored = await redis.get(k);
  if (stored === 'IN_PROGRESS') return res.status(409).set('Retry-After', '1').end();
  const { code, body } = JSON.parse(stored);
  // replay the original response
  return res.status(code).json(body);
}
const { code, body } = await action();
await redis.set(k, JSON.stringify({ code, body }), { EX: 86_400 });
res.status(code).json(body);
