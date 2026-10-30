// Node already has a timer queue; a scheduler adds ids, cancellation, repeats and retries on top.
export class TaskScheduler {
  #timers = new Map();

  schedule(fn, delayMs, { every, retry = { max: 3, baseMs: 200 } } = {}) {
    const id = crypto.randomUUID();
    const run = async (attempt = 1) => {
      try {
        await fn();
        if (every) this.#timers.set(id, setTimeout(run, every));
        else this.#timers.delete(id);
      } catch (err) {
        if (attempt >= retry.max) return this.#timers.delete(id); // dead-letter here
        const delay = Math.random() * retry.baseMs * 2 ** attempt; // full jitter
        this.#timers.set(id, setTimeout(() => run(attempt + 1), delay));
      }
    };
    this.#timers.set(id, setTimeout(run, delayMs));
    return id;
  }

  cancel(id) {
    clearTimeout(this.#timers.get(id));
    this.#timers.delete(id);
  }
}
// Timers live in process memory: a restart loses them. Durable schedules belong in a database/queue.
