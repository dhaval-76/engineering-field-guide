interface RetryPolicy {
    Optional<Duration> delayFor(int failedAttempt, Exception cause);
}

/** Exponential backoff with full jitter: random delay in [0, min(cap, base·2^attempt)]. */
final class ExponentialBackoff implements RetryPolicy {
    private final int maxAttempts;
    private final Duration base, cap;

    ExponentialBackoff(int maxAttempts, Duration base, Duration cap) {
        this.maxAttempts = maxAttempts;
        this.base = base;
        this.cap = cap;
    }

    public Optional<Duration> delayFor(int attempt, Exception cause) {
        if (attempt >= maxAttempts || cause instanceof NonRetryableException)
            return Optional.empty();
        long ceiling = Math.min(cap.toMillis(), base.toMillis() << Math.min(attempt, 20));
        return Optional.of(Duration.ofMillis(ThreadLocalRandom.current().nextLong(ceiling + 1)));
    }
}
// Jitter spreads retries out so thousands of failed tasks don't all retry at the same
// instant and hammer a recovering dependency ("thundering herd").
