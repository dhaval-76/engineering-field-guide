const withTimeout = (p, ms) =>
  Promise.race([p, new Promise((_, reject) => setTimeout(() => reject(new Error(`timeout after ${ms} ms`)), ms))]);

// orTimeout + exceptionally: recover from a failure with a fallback value
const price = withTimeout(pricing.get(id), 200).catch(() => ({ unavailable: true }));

// handle: see either the value or the error, return a new value
const logged = pricing.get(id).then(
  (value) => value,
  (err) => { log.warn({ err, id }, 'pricing failed'); return { unavailable: true }; },
);

// whenComplete: observe without changing the outcome
logged.finally(() => metrics.increment('pricing.calls'));

// A rejection skips following .then(onFulfilled) handlers until a .catch handles it.
// With fetch, prefer AbortSignal.timeout(200): it also cancels the underlying request.
