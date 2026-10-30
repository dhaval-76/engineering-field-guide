// Semaphore: at most 10 concurrent calls to a fragile downstream
Semaphore permits = new Semaphore(10);
if (!permits.tryAcquire(100, MILLISECONDS)) throw new TooBusyException();
try { return partnerApi.call(req); } finally { permits.release(); }

// CountDownLatch: wait until N one-time events have happened (e.g. caches warmed)
CountDownLatch warmed = new CountDownLatch(3);
for (Cache c : caches) pool.submit(() -> { c.warm(); warmed.countDown(); });
if (!warmed.await(30, SECONDS)) log.warn("starting with cold caches");

// CyclicBarrier / Phaser: N threads meet at a point repeatedly (simulations, parallel phases)
CyclicBarrier barrier = new CyclicBarrier(4, () -> mergePartialResults());
