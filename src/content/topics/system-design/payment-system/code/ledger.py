# psycopg 3. Moves `amount` minor units between accounts exactly once per idempotency key.
from psycopg.types.json import Jsonb


class InsufficientFunds(Exception):
    pass


def transfer(conn, idempotency_key: str, src: int, dst: int, amount: int):
    if amount <= 0:
        raise ValueError("amount must be positive")
    with conn.transaction():
        # 1. Claim the key. A retry (or a concurrent duplicate) inserts nothing → return
        # the original journal.
        row = conn.execute(
            "INSERT INTO journal (id, idempotency_key, kind) VALUES (gen_random_uuid(), %s, 'transfer') "
            "ON CONFLICT (idempotency_key) DO NOTHING RETURNING id",
            (idempotency_key,),
        ).fetchone()
        if row is None:
            return conn.execute(
                "SELECT id FROM journal WHERE idempotency_key = %s", (idempotency_key,)
            ).fetchone()[0]
        journal_id = row[0]
        # 2. Lock both accounts in a consistent order — no deadlocks between opposite
        # transfers.
        conn.execute(
            "SELECT id FROM accounts WHERE id = ANY(%s) ORDER BY id FOR UPDATE", ([src, dst],)
        )
        # 3. Debit with a guard; 0 rows → insufficient funds → the whole transaction rolls
        # back.
        if (
            conn.execute(
                "UPDATE accounts SET balance = balance - %s WHERE id = %s AND (allow_negative OR balance >= %s)",
                (amount, src, amount),
            ).rowcount
            == 0
        ):
            raise InsufficientFunds(src)
        conn.execute("UPDATE accounts SET balance = balance + %s WHERE id = %s", (amount, dst))
        # 4. The immutable record: entries sum to zero. 5. Event in the same transaction
        # (outbox).
        conn.execute(
            "INSERT INTO entries (journal_id, account_id, amount) VALUES (%s, %s, %s), (%s, %s, %s)",
            (journal_id, src, -amount, journal_id, dst, amount),
        )
        conn.execute(
            "INSERT INTO outbox (id, type, payload) VALUES (gen_random_uuid(), 'TransferCompleted', %s)",
            (Jsonb({"journal_id": str(journal_id), "amount": amount}),),
        )
        return journal_id
