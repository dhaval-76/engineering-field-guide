// ioredis. A lease-based lock: unique token, expiry, owner-checked release/extend.
import { randomUUID } from 'node:crypto';

const RELEASE = `if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end`;
const EXTEND = `if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end`;

export async function tryAcquire(redis, key, ttlMs) {
  const token = randomUUID();
  const ok = await redis.set(key, token, 'PX', ttlMs, 'NX');
  return ok === 'OK' ? token : null;
}

export async function release(redis, key, token) {
  return (await redis.eval(RELEASE, 1, key, token)) === 1;
}

export async function extend(redis, key, token, ttlMs) {
  return (await redis.eval(EXTEND, 1, key, token, ttlMs)) === 1;
}

// Usage: an efficiency lock — at worst two workers do the job twice.
const token = await tryAcquire(redis, 'lock:report:2026-10-30', 5 * 60_000);
if (token) {
  try {
    await buildDailyReport();
  } finally {
    await release(redis, 'lock:report:2026-10-30', token);
  }
}
