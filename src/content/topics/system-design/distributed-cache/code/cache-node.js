// One cache node, Redis-style: lazy + sampled active expiry, approximate LRU by sampling.
export class CacheNode {
  #items = new Map(); // key -> { value, expiresAt, lastAccess, slot }
  #keys = []; // dense array for O(1) random picks
  evictions = 0;

  constructor(capacity, samples = 5, random = Math.random) {
    Object.assign(this, { capacity, samples, random });
  }

  get(key, now) {
    const it = this.#items.get(key);
    if (!it) return undefined;
    if (it.expiresAt <= now) {
      this.#remove(key); // lazy expiry
      return undefined;
    }
    it.lastAccess = now;
    return it.value;
  }

  set(key, value, ttlMs, now) {
    let it = this.#items.get(key);
    if (!it) {
      if (this.#items.size >= this.capacity) this.#evictOne(now);
      it = { slot: this.#keys.length };
      this.#keys.push(key);
      this.#items.set(key, it);
    }
    Object.assign(it, { value, expiresAt: ttlMs > 0 ? now + ttlMs : Infinity, lastAccess: now });
  }

  #pick() {
    return this.#keys[Math.floor(this.random() * this.#keys.length)];
  }

  #evictOne(now) {
    let victim = null;
    let oldest = Infinity;
    for (let i = 0; i < this.samples; i++) {
      const k = this.#pick();
      const it = this.#items.get(k);
      if (it.expiresAt <= now) {
        victim = k;
        break;
      }
      if (it.lastAccess < oldest) [oldest, victim] = [it.lastAccess, k];
    }
    this.#remove(victim);
    this.evictions++;
  }

  activeExpireCycle(now, sampleSize = 20) {
    let removed = 0;
    let expired;
    do {
      expired = 0;
      const n = Math.min(sampleSize, this.#keys.length);
      for (let i = 0; i < n; i++) {
        const k = this.#pick();
        if (this.#items.get(k).expiresAt <= now) {
          this.#remove(k);
          expired++;
        }
      }
      removed += expired;
    } while (this.#keys.length && expired > sampleSize / 4);
    return removed;
  }

  #remove(key) {
    const it = this.#items.get(key);
    this.#items.delete(key);
    const last = this.#keys.pop();
    if (last !== key) {
      this.#keys[it.slot] = last;
      this.#items.get(last).slot = it.slot;
    }
  }

  get size() {
    return this.#items.size;
  }
}
