// A limiter is any object with tryAcquire(key) → boolean, where key identifies the caller
// (user, API key, IP). Inject the clock so tests don't sleep:
//   new SomeLimiter({ limit: 100, windowMs: 60_000, now: () => fakeTime })
export const systemClock = () => performance.now(); // milliseconds, monotonic
