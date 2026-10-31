/**
 * A task due at `runAtNanos`. DelayQueue orders by remaining delay and only releases due
 * tasks.
 */
final class ScheduledTask implements Delayed {
    final String id;
    final Runnable body;
    final long runAtNanos;
    final Duration repeatEvery;      // null = one-shot
    final int attempt;
    private final Ticker ticker;

    ScheduledTask(String id, Runnable body, long runAtNanos, Duration repeatEvery, int attempt, Ticker ticker) {
        this.id = id; this.body = body; this.runAtNanos = runAtNanos; this.repeatEvery = repeatEvery;
        this.attempt = attempt; this.ticker = ticker;
    }
    public long getDelay(TimeUnit unit) { return unit.convert(runAtNanos - ticker.nanos(), TimeUnit.NANOSECONDS); }
    public int compareTo(Delayed o) { return Long.compare(runAtNanos, ((ScheduledTask) o).runAtNanos); }
    ScheduledTask next(long at, int attempt) { return new ScheduledTask(id, body, at, repeatEvery, attempt, ticker); }
}

final class TaskScheduler implements AutoCloseable {
    private final DelayQueue<ScheduledTask> queue = new DelayQueue<>();
    private final Set<String> cancelled = ConcurrentHashMap.newKeySet();
    private final ExecutorService workers;
    private final Thread dispatcher;
    private final RetryPolicy retry;
    private final Ticker ticker;

    TaskScheduler(int workerThreads, RetryPolicy retry, Ticker ticker) {
        this.workers = Executors.newFixedThreadPool(workerThreads);
        this.retry = retry; this.ticker = ticker;
        this.dispatcher = Thread.ofPlatform().name("scheduler-dispatch").start(this::dispatchLoop);
    }

    String schedule(Runnable body, Duration delay)                   { return add(body, delay, null); }
    String scheduleRepeating(Runnable body, Duration first, Duration every) { return add(body, first, every); }
    // lazy: skipped when it comes due
    void cancel(String id) { cancelled.add(id); }

    private String add(Runnable body, Duration delay, Duration every) {
        String id = UUID.randomUUID().toString();
        queue.put(new ScheduledTask(id, body, ticker.nanos() + delay.toNanos(), every, 1, ticker));
        return id;
    }

    private void dispatchLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // blocks until the earliest task is due
                ScheduledTask t = queue.take();
                // only one instance is ever queued, so skipping ends it
                if (cancelled.remove(t.id)) continue;
                // never run user code on the dispatcher
                workers.execute(() -> run(t));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void run(ScheduledTask t) {
        try {
            t.body.run();
            // fixed rate
            if (t.repeatEvery != null) queue.put(t.next(t.runAtNanos + t.repeatEvery.toNanos(), 1));
        } catch (RuntimeException e) {
            retry.delayFor(t.attempt, e).ifPresentOrElse(
                    d -> queue.put(t.next(ticker.nanos() + d.toNanos(), t.attempt + 1)),
                    () -> deadLetter(t, e));
        }
    }

    private void deadLetter(ScheduledTask t, Exception e) { /* persist for inspection / alert */ }

    @Override public void close() { dispatcher.interrupt(); workers.shutdown(); }
}
