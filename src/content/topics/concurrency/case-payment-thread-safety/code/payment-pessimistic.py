# Pessimistic: lock both rows (SELECT … FOR UPDATE) in a consistent order for the whole
# transaction.
class InsufficientFunds(Exception):
    pass


def transfer(conn, from_id: str, to_id: str, amount: int) -> None:
    with conn.transaction():                 # commit releases both row locks
        # ORDER BY id: every transfer locks rows in the same order → no deadlock
        rows = conn.execute(
            "SELECT id, balance FROM accounts WHERE id = ANY(%s) ORDER BY id FOR UPDATE",
            ([from_id, to_id],),
        ).fetchall()
        balances = dict(rows)
        if balances[from_id] < amount:
            raise InsufficientFunds(from_id)
        move = "UPDATE accounts SET balance = balance + %s WHERE id = %s"
        conn.execute(move, (-amount, from_id))
        conn.execute(move, (amount, to_id))
        conn.execute(
            "INSERT INTO transfers (from_id, to_id, amount) VALUES (%s, %s, %s)",
            (from_id, to_id, amount),
        )
