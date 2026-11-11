interface RateLimiter {
    boolean tryAcquire(String clientId);
}

/** Token bucket: holds up to `capacity` tokens and refills at `perSecond`. Allows bursts. */
class TokenBucketLimiter implements RateLimiter {
    private final double capacity, perSecond;
    private final LongSupplier nanoClock;
    private final Map<String, double[]> buckets = new ConcurrentHashMap<>(); // {tokens, lastNanos}

    TokenBucketLimiter(double capacity, double perSecond, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.perSecond = perSecond;
        this.nanoClock = nanoClock;
    }

    public boolean tryAcquire(String clientId) {
        double[] b =
                buckets.computeIfAbsent(
                        clientId, k -> new double[] {capacity, nanoClock.getAsLong()});
        synchronized (b) { // per-client lock: clients don't block each other
            long now = nanoClock.getAsLong();
            b[0] = Math.min(capacity, b[0] + (now - b[1]) / 1e9 * perSecond); // lazy refill
            b[1] = now;
            if (b[0] < 1) return false;
            b[0] -= 1;
            return true;
        }
    }
}

/** Sliding window log: exact "N per window", at the cost of storing each timestamp. */
class SlidingWindowLimiter implements RateLimiter {
    private final int limit;
    private final long windowNanos;
    private final LongSupplier nanoClock;
    private final Map<String, Deque<Long>> logs = new ConcurrentHashMap<>();

    SlidingWindowLimiter(int limit, long windowNanos, LongSupplier nanoClock) {
        this.limit = limit;
        this.windowNanos = windowNanos;
        this.nanoClock = nanoClock;
    }

    public boolean tryAcquire(String clientId) {
        Deque<Long> log = logs.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        synchronized (log) {
            long now = nanoClock.getAsLong();
            while (!log.isEmpty() && log.peekFirst() <= now - windowNanos) log.pollFirst();
            if (log.size() >= limit) return false;
            log.addLast(now);
            return true;
        }
    }
}
