ThreadPoolExecutor pool = new ThreadPoolExecutor(
        8,                                        // corePoolSize — threads kept alive even when idle
        16,                                       // maximumPoolSize — extra threads only when the QUEUE is full
        60, TimeUnit.SECONDS,                     // idle time before extra (non-core) threads exit
        new ArrayBlockingQueue<>(500),            // BOUNDED queue — the most important choice
        Thread.ofPlatform().name("orders-", 0).factory(),   // named threads show up in dumps and logs
        new ThreadPoolExecutor.CallerRunsPolicy() // when full: the submitting thread runs the task (backpressure)
);

// Rejection policies when queue and max threads are both exhausted:
//   AbortPolicy (default)  → throws RejectedExecutionException — fail fast, return 503
//   CallerRunsPolicy       → slows the producer down by making it do the work
//   DiscardPolicy / DiscardOldestPolicy → silently drop work (rarely acceptable)

// The Executors factory methods hide dangerous defaults:
//   newFixedThreadPool(n)     → UNBOUNDED LinkedBlockingQueue: memory grows without limit under overload
//   newCachedThreadPool()     → UNBOUNDED thread creation: thousands of threads under load
//   newVirtualThreadPerTaskExecutor() → fine for I/O-bound tasks (Java 21+)
