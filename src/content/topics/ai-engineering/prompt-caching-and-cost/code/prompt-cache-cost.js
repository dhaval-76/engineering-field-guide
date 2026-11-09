// Prices are per million tokens. Multipliers apply to the input price.
function costWithoutCache(w, p) {
  const inputTokens = w.requests * (w.prefix + w.suffix);
  return (inputTokens * p.input + w.requests * w.output * p.output) / 1e6;
}

function costWithCache(w, p) {
  const hits = w.requests * w.hitRate;
  const misses = w.requests - hits;
  const prefixUnits = misses * w.prefix * p.writeMultiplier + hits * w.prefix * p.readMultiplier;
  const inputUnits = prefixUnits + w.requests * w.suffix;
  return (inputUnits * p.input + w.requests * w.output * p.output) / 1e6;
}

/** How many cache reads repay one write. */
function readsToBreakEven(p) {
  if (p.writeMultiplier <= 1) return 0;
  return Math.ceil((p.writeMultiplier - 1) / (1 - p.readMultiplier));
}
