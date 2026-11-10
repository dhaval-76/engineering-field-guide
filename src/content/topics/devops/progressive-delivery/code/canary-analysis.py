from dataclasses import dataclass


@dataclass
class Stats:
    requests: int
    errors: int
    p99_millis: float

    def error_rate(self) -> float:
        return 0 if self.requests == 0 else self.errors / self.requests


def judge(stable: Stats, canary: Stats) -> str:
    """Compares the canary with the stable version over the same time window. Judging the
    canary against fixed thresholds alone would blame it for problems the whole system has."""
    if canary.requests < 500:
        return "WAIT"  # too little traffic to tell yet
    # Errors: allow a small absolute margin, so 0 errors vs 1 error isn't a failure.
    if canary.error_rate() > stable.error_rate() + 0.005:
        return "ROLLBACK"
    # Latency: no more than 20% slower at the 99th percentile.
    if canary.p99_millis > stable.p99_millis * 1.2:
        return "ROLLBACK"
    return "PROMOTE"
