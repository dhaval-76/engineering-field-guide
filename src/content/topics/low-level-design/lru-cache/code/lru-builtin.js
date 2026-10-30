// A JS Map iterates in insertion order. Re-inserting a key on access moves it to the end,
// so the first key is always the least recently used.
export class LRUCache {
  #capacity;
  #map = new Map();

  constructor(capacity) {
    this.#capacity = capacity;
  }

  get(key) {
    if (!this.#map.has(key)) return undefined;
    const value = this.#map.get(key);
    this.#map.delete(key);
    this.#map.set(key, value);
    return value;
  }

  put(key, value) {
    this.#map.delete(key);
    this.#map.set(key, value);
    if (this.#map.size > this.#capacity) {
      const oldest = this.#map.keys().next().value;
      this.#map.delete(oldest);
    }
  }
}
