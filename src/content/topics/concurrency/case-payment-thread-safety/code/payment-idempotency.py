# FastAPI: the client sends an Idempotency-Key per payment attempt; retries with the same key replay the result.
from fastapi import Header
from psycopg.types.json import Jsonb

@app.post("/payments")
def pay(req: PaymentRequest, idempotency_key: str = Header(...)):
    return pay_once(idempotency_key, req)

def pay_once(key: str, req) -> dict:
    with pool.connection() as conn:
        with conn.transaction():
            first = conn.execute(          # exactly one request with this key gets to proceed
                "INSERT INTO idempotency_keys (key, request_hash, status) VALUES (%s, %s, 'IN_PROGRESS') "
                "ON CONFLICT (key) DO NOTHING", (key, request_hash(req))).rowcount == 1
            if first:
                result = debit_and_record(conn, req)       # atomic UPDATE + ledger row, same transaction
                conn.execute("UPDATE idempotency_keys SET status = 'DONE', response = %s WHERE key = %s",
                             (Jsonb(result), key))
                return result
    return previous_result_or_conflict(key, req)           # replay, or 409 if in progress / different body
