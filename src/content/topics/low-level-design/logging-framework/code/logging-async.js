// uses Level from above
// Node has one thread, so "async" means: enqueue now, write in batches later, off the request's path.
export class AsyncAppender {
  #buffer = [];
  #scheduled = false;
  dropped = 0;
  closed = false;

  constructor(target, capacity = 10_000) {
    Object.assign(this, { target, capacity });
  }

  append(e) {
    if (this.closed) return;
    if (this.#buffer.length >= this.capacity && e.level < Level.WARN) {
      this.dropped++;                       // full: drop DEBUG/INFO, count it; warnings and errors always queue
      return;
    }
    this.#buffer.push(e);
    if (!this.#scheduled) {
      this.#scheduled = true;
      setImmediate(() => this.#flush());    // one batched write per event-loop turn
    }
  }

  #flush() {
    this.#scheduled = false;
    const batch = this.#buffer;
    this.#buffer = [];
    for (const e of batch) this.target.append(e);
  }

  close() {                                 // call on shutdown so nothing buffered is lost
    this.closed = true;
    this.#flush();
  }
}
