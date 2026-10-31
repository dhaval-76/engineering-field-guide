// Node: timeout + retry with full jitter + a simple circuit breaker (libraries: opossum,
// cockatiel)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export async function withRetry(fn, { attempts = 3, baseMs = 100, maxMs = 1000, retryable }) {
  for (let i = 1; ; i++) {
    try {
      return await fn();
    } catch (err) {
      if (i >= attempts || !retryable(err)) throw err;
      await sleep(Math.random() * Math.min(maxMs, baseMs * 2 ** i));   // full jitter
    }
  }
}

export function circuitBreaker(fn, { failureThreshold = 5, openMs = 20_000 } = {}) {
  let failures = 0, openedAt = 0, state = 'CLOSED';
  return async (...args) => {
    if (state === 'OPEN') {
      if (Date.now() - openedAt < openMs) throw new Error('circuit open');
      state = 'HALF_OPEN';                          // let one trial call through
    }
    try {
      const result = await fn(...args);
      failures = 0; state = 'CLOSED';
      return result;
    } catch (err) {
      if (state === 'HALF_OPEN' || ++failures >= failureThreshold) { state = 'OPEN'; openedAt = Date.now(); }
      throw err;
    }
  };
}

const getStock = circuitBreaker((sku) =>
  withRetry(
    () => fetch(`http://inventory/stock/${sku}`, { signal: AbortSignal.timeout(800) }).then((r) => {
      if (r.status >= 500) throw Object.assign(new Error('upstream'), { retryable: true });
      return r.json();
    }),
    { retryable: (e) => e.retryable || e.name === 'TimeoutError' },
  ),
);
