/**
 * Multiwindow burn-rate alerting (Google SRE Workbook). Burn rate = observed error rate ÷ the
 * error rate the SLO allows. At 14.4 a 30-day budget is gone in about 2 days.
 * Fires only if both the long and the short window burn fast: the long window proves it is
 * serious, the short one proves it is still happening, so alerts also stop quickly.
 */
boolean shouldPage(double slo, double errorRate1h, double errorRate5m) {
    double allowed = 1 - slo; // 99.9% → 0.1% of requests may fail
    return errorRate1h / allowed >= 14.4 && errorRate5m / allowed >= 14.4;
}

/** Share of a 30-day budget a burn rate uses up over a window: burn × window ÷ period. */
double budgetConsumed(double burnRate, double windowHours) {
    return burnRate * windowHours / (30 * 24);
}

/**
 * What Prometheus's histogram_quantile does: find the bucket the quantile falls in and
 * interpolate linearly inside it. upperBounds and cumulative counts are in "le" order.
 */
double histogramQuantile(double q, double[] upperBounds, long[] cumulative) {
    double rank = q * cumulative[cumulative.length - 1];
    for (int i = 0; i < upperBounds.length; i++) {
        if (cumulative[i] >= rank) {
            double lower = i == 0 ? 0 : upperBounds[i - 1];
            long below = i == 0 ? 0 : cumulative[i - 1];
            long inBucket = cumulative[i] - below;
            // the answer can be off by up to the bucket's width
            return lower + (upperBounds[i] - lower) * (rank - below) / inBucket;
        }
    }
    return upperBounds[upperBounds.length - 1];
}
