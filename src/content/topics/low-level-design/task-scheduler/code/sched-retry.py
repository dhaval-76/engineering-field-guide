import random

class NonRetryable(Exception):
    pass

def exponential_backoff(max_attempts: int, base_s: float, cap_s: float):
    """Full jitter: a random delay in [0, min(cap, base·2^attempt)]; None means give up."""
    def delay_for(attempt: int, error: Exception) -> float | None:
        if attempt >= max_attempts or isinstance(error, NonRetryable):
            return None
        return random.uniform(0, min(cap_s, base_s * 2 ** min(attempt, 20)))
    return delay_for
# Jitter spreads retries out so failed tasks don't all retry at once and hammer a
# recovering dependency.
