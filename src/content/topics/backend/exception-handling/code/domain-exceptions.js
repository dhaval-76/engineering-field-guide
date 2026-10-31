// A small hierarchy of business errors, each carrying what the client needs to know.
export class DomainError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code; // stable, documented: "order_not_found"
  }
}
export class NotFoundError extends DomainError {
  constructor(resource, id) {
    super(`${resource}_not_found`, `${resource} ${id} does not exist`);
  }
}
export class ConflictError extends DomainError {}
export class BusinessRuleError extends DomainError {
  constructor(code, message, details = {}) {
    super(code, message);
    this.details = Object.freeze({ ...details });
  }
}

// Services throw meaningful errors and know nothing about HTTP:
throw new BusinessRuleError('insufficient_funds', 'Balance is too low for this payment', { accountId, shortfall });

// One error middleware maps them to responses (RFC 9457 problem details):
const STATUS = new Map([[NotFoundError, 404], [ConflictError, 409], [BusinessRuleError, 422]]);
app.use((err, req, res, next) => {
  const status = [...STATUS].find(([type]) => err instanceof type)?.[1];
  // unexpected → generic 500 handler (logged, no internals leaked)
  if (!status) return next(err);
  res.status(status).type('application/problem+json')
    .json({ type: `https://errors.example.com/${err.code}`, title: err.message, status, code: err.code, ...err.details });
});
