// Semaphore: at most 10 concurrent calls to a fragile downstream (a counter + a queue of
// waiters).
class Semaphore {
  #waiters = [];
  constructor(permits) { this.permits = permits; }
  async acquire() {
    if (this.permits > 0) { this.permits--; return; }
    await new Promise((resolve) => this.#waiters.push(resolve));
  }
  release() {
    const next = this.#waiters.shift();
    if (next) next(); else this.permits++;
  }
}
const permits = new Semaphore(10);

export async function callPartner(req) {
  await permits.acquire();
  try { return await partnerApi.call(req); } finally { permits.release(); }
}

// CountDownLatch: wait until N one-time tasks are done
const results = await Promise.allSettled(caches.map((c) => c.warm()));
if (results.some((r) => r.status === 'rejected')) log.warn('starting with partly cold caches');
