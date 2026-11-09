/** Prices are per million tokens. Multipliers apply to the input price. */
record Pricing(double input, double output, double writeMultiplier, double readMultiplier) {}

record Workload(long requests, long prefix, long suffix, long output, double hitRate) {}

double costWithoutCache(Workload w, Pricing p) {
    double inputTokens = w.requests() * (double) (w.prefix() + w.suffix());
    return (inputTokens * p.input() + w.requests() * (double) w.output() * p.output()) / 1e6;
}

double costWithCache(Workload w, Pricing p) {
    double hits = w.requests() * w.hitRate();
    double misses = w.requests() - hits;
    double prefixUnits =
            misses * w.prefix() * p.writeMultiplier() + hits * w.prefix() * p.readMultiplier();
    double inputUnits = prefixUnits + w.requests() * (double) w.suffix();
    return (inputUnits * p.input() + w.requests() * (double) w.output() * p.output()) / 1e6;
}

/** How many cache reads repay one write. */
int readsToBreakEven(Pricing p) {
    if (p.writeMultiplier() <= 1) return 0;
    return (int) Math.ceil((p.writeMultiplier() - 1) / (1 - p.readMultiplier()));
}
