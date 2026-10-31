ThreadPoolExecutor pool = new ThreadPoolExecutor(
        // corePoolSize — threads kept alive even when idle
        8,
        // maximumPoolSize — extra threads only when the QUEUE is full
        16,
        // idle time before extra (non-core) threads exit
        60, TimeUnit.SECONDS,
        // BOUNDED queue — the most important choice
        new ArrayBlockingQueue<>(500),
        // named threads show up in dumps and logs
        Thread.ofPlatform().name("orders-", 0).factory(),
        // when full: the submitting thread runs the task (backpressure)
        new ThreadPoolExecutor.CallerRunsPolicy()
);

// Rejection policies when queue and max threads are both exhausted:
//   AbortPolicy (default)  → throws RejectedExecutionException — fail fast, return 503
//   CallerRunsPolicy       → slows the producer down by making it do the work
//   DiscardPolicy / DiscardOldestPolicy → silently drop work (rarely acceptable)

// The Executors factory methods hide dangerous defaults:
// newFixedThreadPool(n)     → UNBOUNDED LinkedBlockingQueue: memory grows without limit
// under overload
// newCachedThreadPool()     → UNBOUNDED thread creation: thousands of threads under load
//   newVirtualThreadPerTaskExecutor() → fine for I/O-bound tasks (Java 21+)
