// O(1) LFU: evict the least frequently used key; ties broken by least recently used.
// A JS Set iterates in insertion order, so each bucket is oldest-first.
export class LFUCache {
  #entries = new Map(); // key -> { value, freq }
  #buckets = new Map(); // freq -> Set<key>
  #minFreq = 0;

  constructor(capacity) {
    if (capacity <= 0) throw new Error('capacity must be positive');
    this.capacity = capacity;
  }

  get(key) {
    const e = this.#entries.get(key);
    if (!e) return undefined;
    this.#touch(key, e);
    return e.value;
  }

  put(key, value) {
    const e = this.#entries.get(key);
    if (e) {
      e.value = value;
      this.#touch(key, e);
      return;
    }
    if (this.#entries.size === this.capacity) this.#evict();
    this.#entries.set(key, { value, freq: 1 });
    this.#bucket(1).add(key);
    this.#minFreq = 1;
  }

  #touch(key, e) {
    const bucket = this.#buckets.get(e.freq);
    bucket.delete(key);
    if (bucket.size === 0) {
      this.#buckets.delete(e.freq);
      if (this.#minFreq === e.freq) this.#minFreq = e.freq + 1;
    }
    e.freq += 1;
    this.#bucket(e.freq).add(key);
  }

  #evict() {
    const bucket = this.#buckets.get(this.#minFreq);
    const victim = bucket.values().next().value;
    bucket.delete(victim);
    if (bucket.size === 0) this.#buckets.delete(this.#minFreq);
    this.#entries.delete(victim);
  }

  #bucket(freq) {
    if (!this.#buckets.has(freq)) this.#buckets.set(freq, new Set());
    return this.#buckets.get(freq);
  }

  get size() {
    return this.#entries.size;
  }
}
