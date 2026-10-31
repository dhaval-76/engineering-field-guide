/**
 * Bounded pool that rejects with a domain exception and warns when the queue is filling
 * up.
 */
final class MonitoredExecutor extends ThreadPoolExecutor {
    private final int capacity;

    MonitoredExecutor(int threads, int capacity) {
        super(threads, threads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity),
              (task, executor) -> { throw new ServiceOverloadedException("work queue full"); });
        this.capacity = capacity;
    }

    @Override
    public void execute(Runnable command) {
        int depth = getQueue().size();
        if (depth >= capacity * 0.8) log.warn("queue at {}% ({} / {})", depth * 100 / capacity, depth, capacity);
        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        // execute() failures would otherwise be easy to miss
        if (t != null) log.error("task failed", t);
    }
}
// Export getActiveCount(), getQueue().size() and rejected-task counts as metrics
// (Micrometer can do this).
