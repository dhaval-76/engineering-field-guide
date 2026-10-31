// 1. Single-flight inside one process: concurrent misses for the same key share ONE load.
const inFlight = new Map(); // key -> Promise

export function singleFlight(key, loader) {
  // someone is already loading
  if (inFlight.has(key)) return inFlight.get(key);
  const p = Promise.resolve()
    .then(() => loader(key))
    .finally(() => inFlight.delete(key));
  inFlight.set(key, p);
  return p;
}

// 2. Across instances (ioredis): a short Redis lock so only one instance rebuilds a hot
// key.
const RELEASE_IF_OWNER = `if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end`;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export async function getWithLock(redis, id, load, ttlSeconds = 300) {
  const key = `product:v1:${id}`,
    lockKey = `lock:${key}`;
  for (let attempt = 0; attempt < 50; attempt++) {
    const cached = await redis.get(key);
    if (cached !== null) return JSON.parse(cached);
    const token = crypto.randomUUID(); // identifies OUR lock
    if ((await redis.set(lockKey, token, 'EX', 5, 'NX')) === 'OK') {
      try {
        const value = await load(id);
        const jitter = Math.floor(Math.random() * ttlSeconds * 0.1); // spread expiries
        await redis.set(key, JSON.stringify(value), 'EX', ttlSeconds + jitter);
        return value;
      } finally {
        // release only if still ours
        await redis.eval(RELEASE_IF_OWNER, 1, lockKey, token);
      }
    }
    // someone else is loading: wait, re-check
    await sleep(50);
  }
  // give up waiting; protect latency
  return load(id);
}

// 3. Stale-while-revalidate: store { value, softExpiry } with a longer real TTL. After
// softExpiry,
// return the stale value at once and refresh in the background (guarded by the lock
// above).
