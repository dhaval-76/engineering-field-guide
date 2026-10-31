# psycopg 3. Runs `action` at most once per (client, key); repeats get the stored
# response.
from psycopg.types.json import Jsonb

class IdempotencyKeyReused(Exception): pass      # → 422
class RequestInProgress(Exception): pass         # → 409, retry later

# a final answer (402 declined…) — stored like a success
class BusinessRejection(Exception):
    def __init__(self, code: int, body: dict):
        super().__init__(body.get("message"))
        self.code, self.body = code, body

SAVE = """UPDATE idempotency_keys SET status = 'DONE', response_code = %s, response_body = %s
          WHERE client_id = %s AND key = %s"""

def execute_once(pool, client_id: str, key: str, request_hash: str, action) -> tuple[int, dict]:
    with pool.connection() as conn:
        # 1. Claim the key atomically: the primary key makes concurrent duplicates lose
        # this race.
        claimed = conn.execute(
            """INSERT INTO idempotency_keys (client_id, key, request_hash, status, locked_until)
               VALUES (%s, %s, %s, 'IN_PROGRESS', now() + interval '30 seconds')
               ON CONFLICT (client_id, key) DO NOTHING""", (client_id, key, request_hash)).rowcount
        conn.commit()
        if claimed == 0:
            h, status, code, body, locked = conn.execute(
                """SELECT request_hash, status, response_code, response_body, locked_until > now()
                   FROM idempotency_keys WHERE client_id = %s AND key = %s""", (client_id, key)).fetchone()
            conn.commit()
            if h != request_hash:
                raise IdempotencyKeyReused()
            if status == "DONE":
                # replay the stored response
                return code, body
            if locked:
                raise RequestInProgress()
            taken = conn.execute(                                    # stale: take it over
                """UPDATE idempotency_keys SET locked_until = now() + interval '30 seconds'
                   WHERE client_id = %s AND key = %s AND status = 'IN_PROGRESS' AND locked_until <= now()""",
                (client_id, key)).rowcount
            conn.commit()
            if taken == 0:
                raise RequestInProgress()

        # 2. Do the work and record the outcome in ONE transaction, so they can't
        # disagree.
        try:
            with conn.transaction():
                code, body = action(conn)
                conn.execute(SAVE, (code, Jsonb(body), client_id, key))
            return code, body
        # final answer: store it for retries
        except BusinessRejection as e:
            with conn.transaction():
                conn.execute(SAVE, (e.code, Jsonb(e.body), client_id, key))
            return e.code, e.body
        # unexpected: let a retry run again
        except Exception:
            with conn.transaction():
                conn.execute("DELETE FROM idempotency_keys "
                             "WHERE client_id = %s AND key = %s AND status = 'IN_PROGRESS'",
                             (client_id, key))
            raise
