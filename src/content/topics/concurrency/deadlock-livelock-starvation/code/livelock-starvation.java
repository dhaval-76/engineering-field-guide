// LIVELOCK: two "polite" workers keep backing off in lockstep — busy, but no progress.
while (true) {
    if (!other.isActive()) { doWork(); break; }
    active = false;            // step aside…
    Thread.onSpinWait();
    active = true;             // …and step back in at the same moment as the other thread
}
// Fix: randomised (jittered) backoff, or let one side win deterministically (e.g. lower
// id goes first).

// STARVATION: a non-fair lock lets newly arriving threads "barge" ahead of long waiters.
// default: higher throughput, no ordering guarantee
ReentrantLock unfair = new ReentrantLock();
// FIFO-ish hand-off to the longest waiter, slower under contention
ReentrantLock fair   = new ReentrantLock(true);
// Other causes: a reader-heavy ReadWriteLock starving writers, thread priorities,
// one tenant flooding a shared pool (fix with per-tenant queues or bulkheads).
