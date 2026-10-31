// JS has a built-in Map (insertion-ordered hash table). This shows the same mechanics by
// hand.
export class MiniHashMap {
  #buckets = Array.from({ length: 8 }, () => []);
  #size = 0;

  #index(key) {
    let h = 0;
    // Java-style string hash
    for (const ch of String(key)) h = (Math.imul(31, h) + ch.charCodeAt(0)) | 0;
    h ^= h >>> 16;
    return h & (this.#buckets.length - 1);
  }

  get(key) {
    return this.#buckets[this.#index(key)].find(([k]) => k === key)?.[1];
  }

  set(key, value) {
    const bucket = this.#buckets[this.#index(key)];
    const entry = bucket.find(([k]) => k === key);
    if (entry) { entry[1] = value; return; }
    bucket.push([key, value]);
    if (++this.#size > this.#buckets.length * 0.75) this.#resize();
  }

  #resize() {
    const entries = this.#buckets.flat();
    this.#buckets = Array.from({ length: this.#buckets.length * 2 }, () => []);
    for (const [k, v] of entries) this.#buckets[this.#index(k)].push([k, v]);
  }
}
