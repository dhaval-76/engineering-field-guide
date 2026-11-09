import random

TRANSIENT = {"UNAVAILABLE", "RESOURCE_EXHAUSTED", "DEADLINE_EXCEEDED"}


def downstream_timeout_ms(remaining_ms, reserve_ms, max_ms):
    """The timeout for a downstream call.

    It is what is left of the caller's deadline, minus time kept back for this service's own
    work. Zero or less means the answer would arrive too late: fail now.
    """
    return min(remaining_ms - reserve_ms, max_ms)


def should_retry(status_code, idempotent, attempt, max_attempts, remaining_ms, backoff_ms):
    """Retry only what is transient, safe to repeat, and still has time to finish."""
    return (
        status_code in TRANSIENT
        and idempotent
        and attempt < max_attempts
        and remaining_ms > backoff_ms
    )


def backoff_ms(attempt, base_ms, cap_ms, rng=random):
    """Full jitter: stops many clients from retrying at the same moment."""
    return rng.random() * min(cap_ms, base_ms * 2 ** min(attempt, 20))
