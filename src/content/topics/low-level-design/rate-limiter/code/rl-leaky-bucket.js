// Leaky bucket as a queue: requests wait in a bounded queue and are released at a
// constant rate.
export class LeakyBucket {
  #queue = [];
  #timer;

  constructor({ capacity, perSecond, downstream }) {
    this.capacity = capacity;
    this.#timer = setInterval(() => {
      const next = this.#queue.shift();
      if (next !== undefined) downstream(next);
    // timers have ~1 ms granularity; for high rates release a batch per tick
    }, 1000 / perSecond);
  }

  // false = bucket full; reject (HTTP 429) or shed load
  offer(request) {
    if (this.#queue.length >= this.capacity) return false;
    this.#queue.push(request);
    return true;
  }

  close() {
    clearInterval(this.#timer);
  }
}
