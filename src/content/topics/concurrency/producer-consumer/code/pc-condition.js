// An async bounded queue: producers wait when full, consumers wait when empty — no busy
// polling.
export class BoundedQueue {
  #items = [];
  #notFull = [];   // resolvers of waiting producers
  #notEmpty = [];  // resolvers of waiting consumers
  constructor(capacity) { this.capacity = capacity; }

  async put(item) {
    while (this.#items.length >= this.capacity) await new Promise((r) => this.#notFull.push(r));
    this.#items.push(item);
    this.#notEmpty.shift()?.();          // wake one consumer
  }

  async take() {
    while (this.#items.length === 0) await new Promise((r) => this.#notEmpty.push(r));
    const item = this.#items.shift();
    this.#notFull.shift()?.();           // wake one producer
    return item;
  }
}
