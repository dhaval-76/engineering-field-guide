# psycopg 3. Claim due jobs with a lease and a fencing number; commit the claim before running anything.
import random
import threading

LEASE_SECONDS = 60

class Retryable(Exception):
    pass

def claim(conn, worker_id: str, partitions: list[int], batch: int) -> list:
    with conn.transaction():
        return conn.execute(
            """UPDATE jobs j SET status = 'RUNNING', lease_owner = %s, lease_until = now() + make_interval(secs => %s),
                                 attempts = attempts + 1, fence = fence + 1
               WHERE j.id IN (
                 SELECT id FROM jobs
                 WHERE partition = ANY(%s) AND run_at <= now()
                   AND (status = 'PENDING' OR (status = 'RUNNING' AND lease_until < now()))   -- expired lease
                 ORDER BY priority DESC, run_at LIMIT %s
                 FOR UPDATE SKIP LOCKED)
               RETURNING id, type, payload, attempts, max_attempts, fence""",
            (worker_id, LEASE_SECONDS, partitions, batch)).fetchall()

def run(pool, job, handlers) -> None:
    job_id, job_type, payload, attempts, max_attempts, fence = job
    stop = threading.Event()
    def heartbeat():
        while not stop.wait(20):
            with pool.connection() as c:
                c.execute("UPDATE jobs SET lease_until = now() + make_interval(secs => %s) WHERE id = %s AND fence = %s",
                          (LEASE_SECONDS, job_id, fence))
    threading.Thread(target=heartbeat, daemon=True).start()
    try:
        handlers[job_type](payload, fence)                       # idempotent; fence passed to resources
        finish(pool, job_id, fence, "SUCCEEDED", None, None)
    except Retryable as e:
        exhausted = attempts >= max_attempts
        backoff = min(3600, 2 ** attempts * (0.5 + random.random()))
        finish(pool, job_id, fence, "DEAD" if exhausted else "PENDING", str(e), None if exhausted else backoff)
    except Exception as e:
        finish(pool, job_id, fence, "DEAD", repr(e), None)
    finally:
        stop.set()

def finish(pool, job_id, fence, status, error, retry_in_seconds) -> None:
    """Only the current lease holder (matching fence) may record the outcome."""
    with pool.connection() as c:
        updated = c.execute(
            """UPDATE jobs SET status = %s, last_error = %s, lease_owner = NULL, lease_until = NULL,
                               run_at = COALESCE(now() + make_interval(secs => %s), run_at)
               WHERE id = %s AND fence = %s""", (status, error, retry_in_seconds, job_id, fence)).rowcount
    if updated == 0:
        print(f"lost lease on {job_id} (fence {fence}); result discarded")
