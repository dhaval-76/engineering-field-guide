/** Hands events to a background thread so request threads never wait on disk or network. */
class AsyncAppender implements Appender {
    private final BlockingQueue<LogEvent> queue;
    private final Appender target;
    private final Thread worker;
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean closed;

    AsyncAppender(Appender target, int capacity) {
        this.target = target;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.worker = Thread.ofPlatform().daemon().name("log-writer").start(this::drain);
    }

    public void append(LogEvent e) {
        if (closed) return;
        if (e.level().compareTo(Level.WARN) >= 0) {
            // never drop warnings/errors: wait
            try { queue.put(e); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } else if (!queue.offer(e)) {
            // full: drop DEBUG/INFO, count it
            dropped.incrementAndGet();
        }
    }

    private void drain() {
        try {
            while (!closed || !queue.isEmpty()) {
                LogEvent e = queue.poll(100, TimeUnit.MILLISECONDS);
                if (e != null) target.append(e);
            }
        } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    long dropped() { return dropped.get(); }

    @Override public void close() {                                 // flush on shutdown
        closed = true;
        try { worker.join(5_000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        target.close();
    }
}
