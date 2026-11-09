const TRANSIENT = new Set(["UNAVAILABLE", "RESOURCE_EXHAUSTED", "DEADLINE_EXCEEDED"]);

/**
 * The timeout for a downstream call: what is left of the caller's deadline, minus time kept back
 * for this service's own work. Zero or less means the answer would arrive too late: fail now.
 */
function downstreamTimeoutMs(remainingMs, reserveMs, maxMs) {
  return Math.min(remainingMs - reserveMs, maxMs);
}

/** Retry only what is transient, safe to repeat, and still has time to finish. */
function shouldRetry({ statusCode, idempotent, attempt, maxAttempts, remainingMs, backoffMs }) {
  return (
    TRANSIENT.has(statusCode) && idempotent && attempt < maxAttempts && remainingMs > backoffMs
  );
}

/** Full jitter: stops many clients from retrying at the same moment. */
function backoffMs(attempt, baseMs, capMs, random = Math.random) {
  return random() * Math.min(capMs, baseMs * 2 ** Math.min(attempt, 20));
}
