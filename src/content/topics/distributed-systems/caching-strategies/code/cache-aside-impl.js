// ioredis + node-postgres
const TTL_SECONDS = 600;
const jitter = (s) => Math.round(s * (0.9 + Math.random() * 0.2));

export async function getProduct(id) {
  const key = `product:v1:${id}`;
  const cached = await redis.get(key).catch(() => null);   // cache down → just miss
  if (cached) return JSON.parse(cached);

  const { rows } = await db.query('SELECT id, name, price FROM products WHERE id = $1', [id]);
  if (rows.length === 0) {
    await redis.set(key, 'null', 'EX', 30).catch(() => {}); // negative cache: stops penetration
    return null;
  }
  await redis.set(key, JSON.stringify(rows[0]), 'EX', jitter(TTL_SECONDS)).catch(() => {});
  return rows[0];
}

export async function changePrice(id, price) {
  await db.query('UPDATE products SET price = $1 WHERE id = $2', [price, id]);  // autocommit
  await redis.del(`product:v1:${id}`);                                           // after the commit
}
