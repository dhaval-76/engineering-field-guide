private static final Set<String> TRANSIENT =
        Set.of("UNAVAILABLE", "RESOURCE_EXHAUSTED", "DEADLINE_EXCEEDED");

/**
 * The timeout for a downstream call: what is left of the caller's deadline, minus time kept back
 * for this service's own work. Zero or less means the answer would arrive too late: fail now.
 */
long downstreamTimeoutMs(long remainingMs, long reserveMs, long maxMs) {
    return Math.min(remainingMs - reserveMs, maxMs);
}

/** Retry only what is transient, safe to repeat, and still has time to finish. */
boolean shouldRetry(
        String statusCode,
        boolean idempotent,
        int attempt,
        int maxAttempts,
        long remainingMs,
        long backoffMs) {
    return TRANSIENT.contains(statusCode)
            && idempotent
            && attempt < maxAttempts
            && remainingMs > backoffMs;
}

/** Full jitter: stops many clients from retrying at the same moment. */
long backoffMs(int attempt, long baseMs, long capMs, Random random) {
    long ceiling = Math.min(capMs, baseMs << Math.min(attempt, 20));
    return (long) (random.nextDouble() * ceiling);
}
