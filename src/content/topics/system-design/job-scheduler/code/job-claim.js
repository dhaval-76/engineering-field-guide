// node-postgres. Claim due jobs with a lease and a fencing number; commit the claim
// before running anything.
const LEASE_SECONDS = 60;

export async function claim(pool, workerId, partitions, batch) {
  const { rows } = await pool.query(
    `UPDATE jobs j SET status = 'RUNNING', lease_owner = $1, lease_until = now() + make_interval(secs => $2),
                       attempts = attempts + 1, fence = fence + 1
     WHERE j.id IN (
       SELECT id FROM jobs
       WHERE partition = ANY($3::int[]) AND run_at <= now()
         AND (status = 'PENDING' OR (status = 'RUNNING' AND lease_until < now()))   -- expired lease
       ORDER BY priority DESC, run_at LIMIT $4
       FOR UPDATE SKIP LOCKED)
     RETURNING id, type, payload, attempts, max_attempts, fence`,
    [workerId, LEASE_SECONDS, partitions, batch],
  );
  return rows;
}

export async function run(pool, job, handlers) {
  const heartbeat = setInterval(
    () =>
      pool.query(
        'UPDATE jobs SET lease_until = now() + make_interval(secs => $1) WHERE id = $2 AND fence = $3',
        [LEASE_SECONDS, job.id, job.fence],
      ),
    20_000,
  );
  try {
    // idempotent; fence passed to resources
    await handlers[job.type](job.payload, job.fence);
    await finish(pool, job, 'SUCCEEDED', null, null);
  } catch (e) {
    const retryable = e.retryable !== false;
    const exhausted = job.attempts >= job.max_attempts;
    const backoff = Math.min(3600, 2 ** job.attempts * (0.5 + Math.random()));
    await finish(
      pool,
      job,
      !retryable || exhausted ? 'DEAD' : 'PENDING',
      String(e),
      retryable && !exhausted ? backoff : null,
    );
  } finally {
    clearInterval(heartbeat);
  }
}

// Only the current lease holder (matching fence) may record the outcome.
async function finish(pool, job, status, error, retryInSeconds) {
  const { rowCount } = await pool.query(
    `UPDATE jobs SET status = $1, last_error = $2, lease_owner = NULL, lease_until = NULL,
                     run_at = COALESCE(now() + make_interval(secs => $3), run_at)
     WHERE id = $4 AND fence = $5`,
    [status, error, retryInSeconds, job.id, job.fence],
  );
  if (rowCount === 0)
    console.warn(`lost lease on ${job.id} (fence ${job.fence}); result discarded`);
}
