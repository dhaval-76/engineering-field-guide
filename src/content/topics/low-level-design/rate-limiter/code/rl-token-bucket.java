/** Capacity = largest allowed burst; refill rate = sustained requests per second. */
final class TokenBucketLimiter implements RateLimiter {
    private final long capacity;
    private final double tokensPerNano;
    private final Ticker ticker;
    // Per-key buckets; expire idle ones so the map can't grow forever.
    private final Cache<String, Bucket> buckets =
            Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).build();

    TokenBucketLimiter(long capacity, double perSecond, Ticker ticker) {
        this.capacity = capacity;
        this.tokensPerNano = perSecond / 1e9;
        this.ticker = ticker;
    }

    public boolean tryAcquire(String key) {
        return buckets.get(key, k -> new Bucket(capacity, ticker.nanos())).tryTake();
    }

    private final class Bucket {
        private double tokens;
        private long last;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.last = now;
        }

        // one lock per key, not one global lock
        synchronized boolean tryTake() {
            long now = ticker.nanos();
            // lazy refill
            tokens = Math.min(capacity, tokens + (now - last) * tokensPerNano);
            last = now;
            if (tokens < 1) return false;
            tokens -= 1;
            return true;
        }
    }
}
