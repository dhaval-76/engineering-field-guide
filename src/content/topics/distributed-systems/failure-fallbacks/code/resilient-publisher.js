import fs from 'node:fs';
import path from 'node:path';

// event = { id, key, payload }; the id lets consumers drop duplicates a replay can
// produce. broker = { async send(event) } — e.g. await producer.send(...) with acks; it
// must throw on failure.

/**
 * Durable spool: an append-only log plus a separate "how many are done" offset. Put it on
 * a persistent volume. A crash between the broker's ack and removeHead() re-sends that
 * event (at-least-once).
 */
export class FileSpool {
  constructor(dir) {
    fs.mkdirSync(dir, { recursive: true });
    this.log = path.join(dir, 'spool.log');
    this.offsetFile = path.join(dir, 'spool.offset');
    this.done = fs.existsSync(this.offsetFile) ? Number(fs.readFileSync(this.offsetFile, 'utf8')) : 0;
    const lines = fs.existsSync(this.log) ? fs.readFileSync(this.log, 'utf8').split('\n').filter(Boolean) : [];
    this.queue = lines.slice(this.done).map((line) => {
      const [id, key, ...rest] = line.split('\t');
      return { id, key, payload: rest.join('\t') };
    });
  }

  append(e) {
    fs.appendFileSync(this.log, `${e.id}\t${e.key}\t${e.payload.replaceAll('\n', ' ')}\n`);
    this.queue.push(e);
  }

  peek() {
    return this.queue[0] ?? null;
  }

  removeHead() {
    this.queue.shift();
    this.done++;
    if (this.queue.length === 0) {
      // Reset the offset first, then drop the log: a crash in between replays
      // (duplicates), never skips.
      this.#writeOffset(0);
      fs.rmSync(this.log, { force: true });
      this.done = 0;
    } else {
      this.#writeOffset(this.done);
    }
  }

  #writeOffset(value) {
    const tmp = `${this.offsetFile}.tmp`;
    fs.writeFileSync(tmp, String(value));
    fs.renameSync(tmp, this.offsetFile); // atomic replace
  }

  get size() {
    return this.queue.length;
  }

  pending() {
    return [...this.queue];
  }
}

/** Minimal breaker: opens after N consecutive failures, lets traffic try again after a cool-down. */
export class Breaker {
  constructor(threshold, coolDownMs, clock = Date.now) {
    Object.assign(this, { threshold, coolDownMs, clock, failures: 0, openedAt: 0, state: 'CLOSED' });
  }
  allow() {
    if (this.state === 'OPEN' && this.clock() - this.openedAt >= this.coolDownMs) this.state = 'HALF_OPEN';
    return this.state !== 'OPEN';
  }
  success() {
    this.failures = 0;
    this.state = 'CLOSED';
  }
  failure() {
    if (this.state === 'HALF_OPEN' || ++this.failures >= this.threshold) {
      this.state = 'OPEN';
      this.openedAt = this.clock();
    }
  }
}

/**
 * Publishes when the broker is healthy; otherwise spools durably and replays later,
 * rate-limited. While a key has spooled events, new events for that key queue behind them
 * (per-key order). As with Kafka itself, await one same-key publish before starting the
 * next.
 */
export class ResilientPublisher {
  #spooledPerKey = new Map();
  #tokens = 0;
  #draining = false;

  constructor(broker, spool, breaker, drainPerSecond, clock = Date.now) {
    Object.assign(this, { broker, spool, breaker, drainPerSecond, clock });
    this.lastRefill = clock();
    for (const e of spool.pending()) this.#count(e.key, +1); // after a restart
  }

  async publish(e) {
    if (this.#spooledPerKey.has(e.key) || !this.breaker.allow()) return this.#spoolIt(e);
    try {
      await this.broker.send(e);
      this.breaker.success();
      return 'SENT';
    } catch {
      this.breaker.failure();
      return this.#spoolIt(e);
    }
  }

  #spoolIt(e) {
    this.spool.append(e);
    this.#count(e.key, +1);
    return 'SPOOLED';
  }

  #count(key, delta) {
    const n = (this.#spooledPerKey.get(key) ?? 0) + delta;
    if (n === 0) this.#spooledPerKey.delete(key);
    else this.#spooledPerKey.set(key, n);
  }

  /** Call on a timer (e.g. setInterval every 100 ms). Returns how many events were replayed. */
  async drainOnce() {
    if (this.#draining) return 0; // one drain at a time keeps the spool's order
    this.#draining = true;
    try {
      const now = this.clock();
      this.#tokens = Math.min(this.drainPerSecond, this.#tokens + ((now - this.lastRefill) / 1000) * this.drainPerSecond);
      this.lastRefill = now;
      let sent = 0;
      while (this.#tokens >= 1 && this.spool.size > 0 && this.breaker.allow()) {
        const e = this.spool.peek();
        try {
          await this.broker.send(e);
        } catch {
          this.breaker.failure();
          break;
        }
        this.breaker.success();
        this.spool.removeHead();
        this.#count(e.key, -1);
        this.#tokens--;
        sent++;
      }
      return sent;
    } finally {
      this.#draining = false;
    }
  }

  get backlog() {
    return this.spool.size;
  }
}
