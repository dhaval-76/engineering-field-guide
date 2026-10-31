// ioredis. Opaque random session ids; reads slide the expiry atomically.
import { randomBytes } from 'node:crypto';
const TTL_SECONDS = 30 * 60;
const key = (id) => `session:${id}`;

export async function createSession(redis, data) {
  const id = randomBytes(32).toString('base64url');                 // unguessable id
  await redis.set(key(id), JSON.stringify(data), 'EX', TTL_SECONDS);
  return id;
}

// Read and slide the expiry in ONE atomic command (GETEX, Redis 6.2+).
export async function touchSession(redis, id) {
  const raw = await redis.getex(key(id), 'EX', TTL_SECONDS);
  return raw === null ? null : JSON.parse(raw);
}

export const invalidateSession = (redis, id) => redis.del(key(id)); // logout
// express-session + connect-redis does this for you in Express apps.
