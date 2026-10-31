// A single node's in-memory store with TTL. Value and expiry live together, so overwrites can't leave stale TTLs.
export class KvNode {
  #data = new Map(); // key -> { value, expiresAt } (0 = no expiry)

  constructor(now = () => Date.now()) {
    this.now = now;
  }

  get(key) {
    const e = this.#data.get(key);
    if (!e) return undefined;
    if (e.expiresAt && this.now() >= e.expiresAt) {
      this.#data.delete(key); // lazy expiry
      return undefined;
    }
    return e.value;
  }

  put(key, value, ttlMs = 0) {
    this.#data.set(key, { value, expiresAt: ttlMs ? this.now() + ttlMs : 0 }); // replaces value AND expiry together
  }

  delete(key) {
    this.#data.delete(key);
  }

  // Active expiry: check a bounded number of keys per pass (Redis samples similarly).
  sweep(maxChecks) {
    const now = this.now();
    let removed = 0, checked = 0;
    for (const [key, e] of this.#data) {
      if (checked++ >= maxChecks) break;
      if (e.expiresAt && now >= e.expiresAt) {
        this.#data.delete(key);
        removed++;
      }
    }
    return removed;
  }
}
