export class NonRetryableError extends Error {}

// Exponential backoff with full jitter: random delay in [0, min(cap, base·2^attempt)];
// null = give up.
export const exponentialBackoff = ({ maxAttempts, baseMs, capMs }) => (attempt, error) => {
  if (attempt >= maxAttempts || error instanceof NonRetryableError) return null;
  const ceiling = Math.min(capMs, baseMs * 2 ** Math.min(attempt, 20));
  return Math.floor(Math.random() * (ceiling + 1));
};
// Jitter spreads retries out so thousands of failed tasks don't retry at the same instant
// and hammer a recovering dependency ("thundering herd").
