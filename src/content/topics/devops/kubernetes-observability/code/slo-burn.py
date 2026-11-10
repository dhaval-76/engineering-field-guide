def should_page(slo: float, error_rate_1h: float, error_rate_5m: float) -> bool:
    """Multiwindow burn-rate alerting (Google SRE Workbook). Burn rate = observed error rate
    divided by the error rate the SLO allows. At 14.4 a 30-day budget is gone in about 2 days.
    Fires only if both windows burn fast: the long window proves it is serious, the short one
    proves it is still happening, so alerts also stop quickly."""
    allowed = 1 - slo  # 99.9% -> 0.1% of requests may fail
    return error_rate_1h / allowed >= 14.4 and error_rate_5m / allowed >= 14.4


def budget_consumed(burn_rate: float, window_hours: float) -> float:
    """Share of a 30-day budget a burn rate uses up over a window: burn x window / period."""
    return burn_rate * window_hours / (30 * 24)


def histogram_quantile(q: float, upper_bounds: list[float], cumulative: list[int]) -> float:
    """What Prometheus's histogram_quantile does: find the bucket the quantile falls in and
    interpolate linearly inside it. upper_bounds and cumulative counts are in "le" order."""
    rank = q * cumulative[-1]
    for i, bound in enumerate(upper_bounds):
        if cumulative[i] >= rank:
            lower = 0 if i == 0 else upper_bounds[i - 1]
            below = 0 if i == 0 else cumulative[i - 1]
            # the answer can be off by up to the bucket's width
            return lower + (bound - lower) * (rank - below) / (cumulative[i] - below)
    return upper_bounds[-1]
