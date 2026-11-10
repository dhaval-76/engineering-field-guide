const errorRate = (s) => (s.requests === 0 ? 0 : s.errors / s.requests);

/**
 * Compares the canary with the stable version over the same time window. Judging the canary
 * against fixed thresholds alone would blame it for problems the whole system has.
 * Each argument is { requests, errors, p99Millis }.
 */
function judge(stable, canary) {
  if (canary.requests < 500) return 'WAIT'; // too little traffic to tell yet
  // Errors: allow a small absolute margin, so 0 errors vs 1 error isn't a failure.
  if (errorRate(canary) > errorRate(stable) + 0.005) return 'ROLLBACK';
  // Latency: no more than 20% slower at the 99th percentile.
  if (canary.p99Millis > stable.p99Millis * 1.2) return 'ROLLBACK';
  return 'PROMOTE';
}
