/** Fixed window: one counter per (key, window). Cheap, but allows 2× the limit across a boundary. */
final class FixedWindowLimiter implements RateLimiter {
    private final int limit; private final long windowNanos; private final Ticker ticker;
    // {windowStart, count}
    private final ConcurrentHashMap<String, long[]> state = new ConcurrentHashMap<>();

    FixedWindowLimiter(int limit, Duration window, Ticker ticker) { this.limit = limit; this.windowNanos = window.toNanos(); this.ticker = ticker; }

    public boolean tryAcquire(String key) {
        long window = ticker.nanos() / windowNanos;
        boolean[] allowed = new boolean[1];
        state.compute(key, (k, s) -> {                     // compute() is atomic per key
            if (s == null || s[0] != window) s = new long[]{window, 0};
            if (s[1] < limit) { s[1]++; allowed[0] = true; }
            return s;
        });
        return allowed[0];
    }
}

/** Sliding window log: exact, but stores one timestamp per allowed request. */
final class SlidingLogLimiter implements RateLimiter {
    private final int limit; private final long windowNanos; private final Ticker ticker;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> logs = new ConcurrentHashMap<>();

    SlidingLogLimiter(int limit, Duration window, Ticker ticker) { this.limit = limit; this.windowNanos = window.toNanos(); this.ticker = ticker; }

    public boolean tryAcquire(String key) {
        ArrayDeque<Long> log = logs.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            long now = ticker.nanos();
            while (!log.isEmpty() && now - log.peekFirst() >= windowNanos) log.pollFirst();
            if (log.size() >= limit) return false;
            log.addLast(now);
            return true;
        }
    }
}

/**
 * Sliding window counter: two counters per key; estimate the rolling count by weighting
 * the previous window by how much of it still overlaps. O(1) memory, smooths the boundary
 * burst.
 */
final class SlidingWindowCounterLimiter implements RateLimiter {
    private final int limit; private final long windowNanos; private final Ticker ticker;
    // {window, current, previous}
    private final ConcurrentHashMap<String, long[]> state = new ConcurrentHashMap<>();

    SlidingWindowCounterLimiter(int limit, Duration window, Ticker ticker) { this.limit = limit; this.windowNanos = window.toNanos(); this.ticker = ticker; }

    public boolean tryAcquire(String key) {
        long now = ticker.nanos(), window = now / windowNanos;
        double elapsedFraction = (now % windowNanos) / (double) windowNanos;
        boolean[] allowed = new boolean[1];
        state.compute(key, (k, s) -> {
            if (s == null) s = new long[]{window, 0, 0};
            if (s[0] != window) {                             // roll windows forward
                s[2] = (s[0] == window - 1) ? s[1] : 0;
                s[1] = 0; s[0] = window;
            }
            double estimate = s[2] * (1 - elapsedFraction) + s[1];
            if (estimate < limit) { s[1]++; allowed[0] = true; }
            return s;
        });
        return allowed[0];
    }
}
