// Fixed window: one counter per (key, window). Cheap, but allows 2× the limit across a
// boundary.
export class FixedWindowLimiter {
  #state = new Map(); // key -> { window, count }
  constructor({ limit, windowMs, now = () => performance.now() }) {
    Object.assign(this, { limit, windowMs, now });
  }
  tryAcquire(key) {
    const window = Math.floor(this.now() / this.windowMs);
    let s = this.#state.get(key);
    if (!s || s.window !== window) this.#state.set(key, (s = { window, count: 0 }));
    if (s.count >= this.limit) return false;
    s.count++;
    return true;
  }
}

// Sliding window log: exact, but stores one timestamp per allowed request.
export class SlidingLogLimiter {
  #logs = new Map();
  constructor({ limit, windowMs, now = () => performance.now() }) {
    Object.assign(this, { limit, windowMs, now });
  }
  tryAcquire(key) {
    const t = this.now();
    const log = this.#logs.get(key) ?? [];
    while (log.length && t - log[0] >= this.windowMs) log.shift();
    this.#logs.set(key, log);
    if (log.length >= this.limit) return false;
    log.push(t);
    return true;
  }
}

// Sliding window counter: weight the previous window by how much of it still overlaps.
// O(1) memory.
export class SlidingWindowCounterLimiter {
  #state = new Map(); // key -> { window, current, previous }
  constructor({ limit, windowMs, now = () => performance.now() }) {
    Object.assign(this, { limit, windowMs, now });
  }
  tryAcquire(key) {
    const t = this.now();
    const window = Math.floor(t / this.windowMs);
    const elapsed = (t % this.windowMs) / this.windowMs;
    let s = this.#state.get(key);
    if (!s) this.#state.set(key, (s = { window, current: 0, previous: 0 }));
    if (s.window !== window) {                                  // roll windows forward
      s.previous = s.window === window - 1 ? s.current : 0;
      s.current = 0;
      s.window = window;
    }
    if (s.previous * (1 - elapsed) + s.current >= this.limit) return false;
    s.current++;
    return true;
  }
}
