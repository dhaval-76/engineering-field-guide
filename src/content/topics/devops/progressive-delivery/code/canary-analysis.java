record Stats(long requests, long errors, double p99Millis) {
    double errorRate() {
        return requests == 0 ? 0 : (double) errors / requests;
    }
}

enum Verdict { PROMOTE, WAIT, ROLLBACK }

/**
 * Compares the canary with the stable version over the same time window. Judging the canary
 * against fixed thresholds alone would blame it for problems the whole system has.
 */
Verdict judge(Stats stable, Stats canary) {
    if (canary.requests() < 500) return Verdict.WAIT; // too little traffic to tell yet
    // Errors: allow a small absolute margin, so 0 errors vs 1 error isn't a failure.
    if (canary.errorRate() > stable.errorRate() + 0.005) return Verdict.ROLLBACK;
    // Latency: no more than 20% slower at the 99th percentile.
    if (canary.p99Millis() > stable.p99Millis() * 1.2) return Verdict.ROLLBACK;
    return Verdict.PROMOTE;
}
