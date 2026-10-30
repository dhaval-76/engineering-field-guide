export class TokenBucketLimiter {
  #buckets = new Map(); // in production evict idle keys (LRU/TTL)

  constructor({ capacity, perSecond, now = () => performance.now() }) {
    Object.assign(this, { capacity, perMs: perSecond / 1000, now });
  }

  tryAcquire(key) {
    const t = this.now();
    const b = this.#buckets.get(key) ?? { tokens: this.capacity, last: t };
    b.tokens = Math.min(this.capacity, b.tokens + (t - b.last) * this.perMs);
    b.last = t;
    const allowed = b.tokens >= 1;
    if (allowed) b.tokens -= 1;
    this.#buckets.set(key, b);
    return allowed; // single-threaded event loop: no lock needed
  }
}
