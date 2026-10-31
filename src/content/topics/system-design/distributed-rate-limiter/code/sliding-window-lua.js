// ioredis: register the same Lua script once as a command (sent with EVALSHA afterwards)
import Redis from 'ioredis';
import { readFileSync } from 'node:fs';

const redis = new Redis({ commandTimeout: 5 });
redis.defineCommand('slidingWindow', {
  numberOfKeys: 1,
  // the script from the Java tab
  lua: readFileSync(new URL('./sliding-window.lua', import.meta.url), 'utf8'),
});

export async function check(key, limit, windowSeconds) {
  try {
    const [allowed, remaining, retryAfter] = await redis.slidingWindow(`rl:${key}`, limit, windowSeconds);
    return { allowed: allowed === 1, remaining, retryAfter };
  } catch {
    // fail-open on limiter errors
    return { allowed: true, remaining: -1, retryAfter: 0 };
  }
}

// Express middleware
export const rateLimit = (limit, windowSeconds) => async (req, res, next) => {
  const d = await check(req.get('x-api-key') ?? req.ip, limit, windowSeconds);
  res.set('X-RateLimit-Limit', String(limit));
  if (d.remaining >= 0) res.set('X-RateLimit-Remaining', String(d.remaining));
  if (!d.allowed) return res.set('Retry-After', String(d.retryAfter)).status(429).end();
  next();
};
