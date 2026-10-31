# psycopg 3
import random, time
from psycopg import errors, IsolationLevel

RETRYABLE = (errors.SerializationFailure, errors.DeadlockDetected)   # 40001, 40P01

def with_serializable_tx(pool, work, max_attempts=5):
    for attempt in range(1, max_attempts + 1):
        try:
            with pool.connection() as conn:
                conn.isolation_level = IsolationLevel.SERIALIZABLE
                with conn.transaction():
                    return work(conn)             # redo every read on each attempt
        except RETRYABLE:
            if attempt == max_attempts:
                raise
            # jittered backoff
            time.sleep(random.uniform(0, min(1.0, 0.02 * 2 ** attempt)))

# with_serializable_tx(pool, lambda conn: take_off_call(conn, "alice", 1))
