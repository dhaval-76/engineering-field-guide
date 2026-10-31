// Graceful shutdown: stop accepting work, let running tasks finish, then force if needed.
void close(ExecutorService pool) {
    // no new tasks; queued ones still run
    pool.shutdown();
    try {
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            List<Runnable> dropped = pool.shutdownNow();   // interrupts running tasks
            log.warn("{} tasks never started", dropped.size());
        }
    } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
// Since Java 19, ExecutorService is AutoCloseable: try (var pool = …) { … } waits for
// tasks on close. In Spring, declare executors as beans (ThreadPoolTaskExecutor) so the
// container shuts them down.
