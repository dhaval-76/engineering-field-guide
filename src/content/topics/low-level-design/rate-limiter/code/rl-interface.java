public interface RateLimiter {
    /** True if the request identified by `key` (user, API key, IP) may proceed now. */
    boolean tryAcquire(String key);
}

/** Time source in nanoseconds, injectable so tests don't sleep. */
@FunctionalInterface
interface Ticker { long nanos(); Ticker SYSTEM = System::nanoTime; }
