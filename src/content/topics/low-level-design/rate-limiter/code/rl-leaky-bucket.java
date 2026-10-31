/**
 * Leaky bucket as a queue: requests wait in a bounded queue and are released at a
 * constant rate. Output is perfectly smooth — useful in front of a downstream that can
 * only take N calls/second.
 */
final class LeakyBucket<T> implements AutoCloseable {
    private final BlockingQueue<T> queue;
    private final ScheduledExecutorService drainer = Executors.newSingleThreadScheduledExecutor();

    LeakyBucket(int queueCapacity, int perSecond, Consumer<T> downstream) {
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        long periodMicros = 1_000_000L / perSecond;
        drainer.scheduleAtFixedRate(() -> {
            T next = queue.poll();
            if (next != null) downstream.accept(next);
        }, 0, periodMicros, TimeUnit.MICROSECONDS);
    }

    /** False = bucket full; the caller should reject (HTTP 429) or shed load. */
    boolean offer(T request) { return queue.offer(request); }

    @Override public void close() { drainer.shutdown(); }
}
