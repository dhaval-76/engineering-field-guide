// Express: one error-handling middleware (four arguments), registered after all routes
class DomainError extends Error {
  constructor(status, code, message, details = {}) {
    super(message);
    Object.assign(this, { status, code, details });
  }
}

app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  const known = err instanceof DomainError;
  // log the stack, don't return it
  if (!known) req.log.error({ err }, 'unhandled error');
  const status = known ? err.status : 500;
  res.status(status).type('application/problem+json').json({
    type: `https://api.example.com/problems/${known ? err.code : 'internal_error'}`,
    title: known ? err.message : 'Something went wrong',
    status,
    ...(known ? err.details : {}),
    traceId: req.id,
  });
});
// Express 5 forwards rejected promises from async handlers to this middleware
// automatically.
