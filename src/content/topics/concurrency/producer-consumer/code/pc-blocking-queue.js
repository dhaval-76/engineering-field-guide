// Single-threaded Node: an async queue with a bounded size and N concurrent consumers.
class AsyncQueue {
  #items = []; #takers = []; #putters = [];
  constructor(capacity) { this.capacity = capacity; }

  async put(item) {
    while (this.#items.length >= this.capacity) await new Promise((r) => this.#putters.push(r));
    this.#items.push(item);
    this.#takers.shift()?.();
  }

  async take() {
    while (this.#items.length === 0) await new Promise((r) => this.#takers.push(r));
    const item = this.#items.shift();
    this.#putters.shift()?.();
    return item;
  }
}

const queue = new AsyncQueue(1000);
for (let i = 0; i < 4; i++) {
  (async () => { for (;;) await fulfil(await queue.take()); })();   // 4 consumers
}
