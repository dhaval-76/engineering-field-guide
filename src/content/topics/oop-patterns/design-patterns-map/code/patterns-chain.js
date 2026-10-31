// Chain of Responsibility: each handler handles, rejects, or calls next() — exactly how Express middleware works.
export function pipeline(handlers, endpoint) {
  const run = (i, req) => (i === handlers.length ? endpoint(req) : handlers[i](req, (r) => run(i + 1, r)));
  return (req) => run(0, req);
}

const auth = (req, next) => (req.hasValidToken ? next(req) : { status: 401 });
const rateLimit = (req, next) => (limiter.tryAcquire(req.clientId) ? next(req) : { status: 429 });
// pipeline([auth, rateLimit], controller.handle)(request);
