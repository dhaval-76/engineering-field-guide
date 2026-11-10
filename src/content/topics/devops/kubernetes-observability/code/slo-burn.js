/**
 * Multiwindow burn-rate alerting (Google SRE Workbook). Burn rate = observed error rate ÷ the
 * error rate the SLO allows. At 14.4 a 30-day budget is gone in about 2 days.
 * Fires only if both the long and the short window burn fast: the long window proves it is
 * serious, the short one proves it is still happening, so alerts also stop quickly.
 */
function shouldPage(slo, errorRate1h, errorRate5m) {
  const allowed = 1 - slo; // 99.9% → 0.1% of requests may fail
  return errorRate1h / allowed >= 14.4 && errorRate5m / allowed >= 14.4;
}

/** Share of a 30-day budget a burn rate uses up over a window: burn × window ÷ period. */
function budgetConsumed(burnRate, windowHours) {
  return (burnRate * windowHours) / (30 * 24);
}

/**
 * What Prometheus's histogram_quantile does: find the bucket the quantile falls in and
 * interpolate linearly inside it. upperBounds and cumulative counts are in "le" order.
 */
function histogramQuantile(q, upperBounds, cumulative) {
  const rank = q * cumulative[cumulative.length - 1];
  for (let i = 0; i < upperBounds.length; i++) {
    if (cumulative[i] >= rank) {
      const lower = i === 0 ? 0 : upperBounds[i - 1];
      const below = i === 0 ? 0 : cumulative[i - 1];
      // the answer can be off by up to the bucket's width
      return lower + ((upperBounds[i] - lower) * (rank - below)) / (cumulative[i] - below);
    }
  }
  return upperBounds[upperBounds.length - 1];
}
